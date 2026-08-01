# PER-26 — Custom Keycloak UI, auto login và self-registration

## 1. Thay đổi đã triển khai

### Trải nghiệm SPA

`AuthProvider` dùng `onLoad: login-required`. Khi mở một protected URL mà chưa
có SSO session, Keycloak JS chuyển thẳng tới Identity Provider bằng
Authorization Code + PKCE S256. Màn hình “Continue to sign in” trung gian đã bị
loại bỏ.

`ProtectedRoute` chỉ còn hai trạng thái:

- Loading skeleton trong lúc adapter khởi tạo/chuyển trang.
- Render ứng dụng sau khi authenticated.

Fallback effect gọi `login()` nếu adapter init xong nhưng chưa authenticated,
giúp tránh màn hình trắng khi browser/session có trạng thái biên.

### Keycloak realm

- `registrationAllowed: true`.
- `resetPasswordAllowed: true`, `rememberMe: true`.
- `USER` thuộc default realm role, nên tài khoản đăng ký mới có quyền workspace
  cơ bản mà không cần admin gán thủ công.
- Login theme: `personal-workspace`.
- Client vẫn là public client, standard flow bật, direct grant/service account
  tắt, PKCE S256 bắt buộc.
- Redirect URI và web origin local được allowlist chính xác cho:
  - `http://localhost:5173/*`
  - `http://127.0.0.1:5173/*`

### Custom theme

Theme nằm tại `keycloak/themes/personal-workspace/login` và được mount read-only
vào container. Theme kế thừa `keycloak.v2`, chỉ override CSS:

- Brand gradient/logo badge, màu indigo, typography và card.
- Input focus state, primary button và info/register area.
- Responsive mobile và `prefers-reduced-motion`.

Không copy các FreeMarker template mặc định. Nhờ vậy khi nâng Keycloak, security
fix và markup mới từ parent theme vẫn được thừa hưởng.

## 2. Luồng xác thực

```text
Browser mở /tasks
  -> Keycloak adapter login-required
  -> redirect /authorize + code_challenge S256
  -> login hoặc register trong custom Keycloak theme
  -> redirect SPA với authorization code
  -> adapter đổi code + verifier lấy token
  -> Axios gắn Bearer token
  -> Gateway và service verify JWT
```

Password chỉ được nhập trên origin Keycloak. SPA không nhìn thấy, lưu hoặc gửi
password.

## 3. Trả lời câu hỏi kiến thức

### Theme inheritance hoạt động thế nào và làm sao upgrade-safe?

`parent=keycloak.v2` yêu cầu Keycloak tìm resource chưa được override từ parent.
Chỉ override CSS và asset branding là ít coupling nhất. Copy `login.ftl`,
`register.ftl` sẽ cho phép đổi markup sâu hơn nhưng dễ vỡ khi Keycloak thêm field,
WebAuthn, passkey hoặc security fix. Nếu buộc phải override template, cần diff
template upstream mỗi lần nâng version và có login/registration E2E tests.

### Vì sao redirect login tốt hơn tự xây password form trong SPA?

Redirect giữ credential trong security boundary của Identity Provider, hỗ trợ
MFA, passkey, account recovery, brute-force protection và federation mà SPA
không cần xử lý password. SPA public client không thể giữ secret. Authorization
Code + PKCE chống authorization-code interception và là flow phù hợp browser.

### Self-registration cần bảo vệ thế nào?

Local development bật registration trực tiếp. Production công khai nên cân nhắc:

- Verify email và cấu hình SMTP.
- CAPTCHA/rate limit/WAF cho registration và password reset.
- Keycloak brute-force detection.
- Terms/privacy consent, disposable-email policy nếu cần.
- Audit events và alert khi đăng ký tăng bất thường.
- Không cấp role đặc quyền làm default.

Nếu workspace chỉ dành cho tổ chức, thay self-registration bằng invitation hoặc
identity federation.

### Default role khác role mapping trong access token thế nào?

Default role được gán vào user khi user được tạo. Trong realm này default
composite chứa `USER`. Protocol mapper/Keycloak token building sau đó đưa realm
role của user vào claim `realm_access.roles`. Backend đọc claim đó thành
`ROLE_USER`. Gán default role không tự đảm bảo token có claim nếu mapper/client
scope bị thay đổi; hai phần phải được kiểm tra độc lập.

### Redirect URI và web origin nên giới hạn ra sao?

Redirect URI quyết định nơi authorization server được phép trả code; wildcard
rộng có thể tạo open redirect/token leakage. Production phải dùng HTTPS và origin
cụ thể, ví dụ `https://workspace.example.com/*`, không dùng `*` toàn domain hoặc
localhost. Web origin điều khiển CORS và cũng phải là exact trusted origin.
Localhost/127 chỉ tồn tại trong cấu hình development.

### Rollout và cache-bust theme thế nào?

Development mode giảm cache và mount theme trực tiếp. Production nên:

1. Đóng gói theme versioned vào custom Keycloak image thay vì bind mount.
2. Build/push immutable image tag.
3. Deploy canary, smoke-test login/register/reset/MFA.
4. Roll rolling update.
5. Nếu asset name thay đổi, dùng filename có content hash hoặc tăng theme/image
   version để browser/CDN không giữ CSS cũ.

Không nên sửa theme trực tiếp trong container đang chạy vì thay đổi không lặp
lại được ở môi trường tiếp theo.

## 4. Bằng chứng kiểm thử

- Realm JSON parse pass.
- `docker compose config --quiet` pass.
- Frontend `npm run check`: Oxlint, 5 Vitest tests, TypeScript và Vite build pass.
- OIDC discovery runtime trả HTTP 200.
- Authorization endpoint với redirect `127.0.0.1:5173` trả HTTP 200.
- Login HTML load `/login/personal-workspace/css/login.css`.
- Login HTML có Register và Forgot Password link.
- Runtime realm: registration/theme/remember/reset-password đều enabled.
- Default realm role composites chứa `USER`.

## 5. Production checklist

- Thay admin bootstrap password và MinIO/database secrets bằng secret manager.
- Dùng PostgreSQL riêng cho Keycloak; hiện local container dùng dev database.
- Bật HTTPS/hostname strict/proxy headers đúng ingress.
- Cấu hình SMTP + verify email trước khi public registration.
- Bật brute-force protection và audit event retention.
- Đóng gói theme vào immutable Keycloak image.
