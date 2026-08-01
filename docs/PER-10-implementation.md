# PER-10 — API Gateway và Keycloak Security

## 1. Mục tiêu và trạng thái triển khai

PER-10 tạo một public entry point tại port `8080`, dùng Keycloak làm Identity Provider
và buộc từng backend service tự kiểm tra access token. Thiết kế không chuyển business logic
vào Gateway và không tin bất kỳ header ownership nào do client gửi.

Đã triển khai:

- module Maven `api-gateway` dùng Spring Cloud Gateway Server WebFlux;
- route Task API tới `task-service:8081`;
- route File API cấu hình qua `FILE_SERVICE_URI`, mặc định port `8082`;
- correlation ID, connect timeout, response timeout và error body chuẩn
  `application/problem+json`;
- Keycloak realm `personal-workspace`, public client `workspace-web`, PKCE S256,
  roles `USER`/`ADMIN`, audience `workspace-api` và hai tài khoản local;
- Gateway và Task Service cùng validate chữ ký, thời gian, issuer và audience của JWT;
- map `realm_access.roles` thành Spring authorities;
- Task/List/Tag controllers lấy `owner_id` từ claim `sub`, xóa hoàn toàn
  `X-Owner-Id` khỏi security boundary;
- Swagger khai báo Bearer JWT;
- CORS giới hạn frontend local `http://localhost:5173`;
- test cho `401`, `403`, route, correlation ID và owner isolation.

File Service chưa tồn tại trong repository tại thời điểm thực hiện. Gateway route đã sẵn sàng,
nhưng không tạo một File Service giả vì việc đó vượt phạm vi và sẽ tạo kiến trúc không đúng.
Khi File Service được thêm, service đó phải cài Resource Server giống Task Service để thỏa điều
kiện “gọi trực tiếp vẫn từ chối token sai”.

## 2. Kiến trúc sau thay đổi

```text
React SPA :5173
    │ Authorization Code + PKCE
    ▼
Keycloak :8090 ── phát access token JWT
    │
    ▼ Bearer JWT
API Gateway :8080
    ├── validate iss/aud/time/signature/role
    ├── X-Correlation-Id + CORS + timeout
    ├── /api/v1/tasks|task-lists|task-tags/** ──► Task Service :8081
    └── /api/v1/files/** ──────────────────────► File Service :8082 (chưa có)

Task Service :8081
    ├── validate lại JWT, không tin Gateway
    ├── owner_id = UUID(JWT.sub)
    └── service → repository → PostgreSQL
```

Gateway chỉ xử lý cross-cutting concern. Controller/service/repository của Task Service vẫn giữ
nguyên boundary đã xây dựng ở PER-7.

## 3. Chi tiết authentication và authorization

### 3.1 Luồng đăng nhập

`workspace-web` là public client nên không có client secret trong trình duyệt. SPA sinh
`code_verifier`, gửi `code_challenge=S256(code_verifier)` đến authorization endpoint.
Sau khi user đăng nhập, Keycloak redirect về SPA với authorization code. SPA đổi code và
`code_verifier` lấy token.

Không bật password grant (`directAccessGrantsEnabled=false`) vì SPA không được thu password và
flow này không còn là lựa chọn hiện đại cho browser application.

### 3.2 JWT validation

Cả Gateway và Task Service kiểm tra độc lập:

1. chữ ký bằng public key lấy từ JWKS endpoint;
2. `iss` đúng `http://localhost:8090/realms/personal-workspace`;
3. token chưa hết hạn và đã có hiệu lực;
4. `aud` chứa `workspace-api`;
5. `realm_access.roles` chứa `USER` hoặc `ADMIN`.

Thiếu/không hợp lệ ở bước 1–4 trả `401 Unauthorized`. Token hợp lệ nhưng không có role cần thiết
trả `403 Forbidden`.

### 3.3 Owner isolation

Trước PER-10, controller nhận `X-Owner-Id` và có dev owner mặc định. Client có thể tự chọn UUID,
do đó cơ chế chỉ phù hợp giai đoạn phát triển.

Sau PER-10:

```java
UUID ownerId = UUID.fromString(jwt.getSubject());
```

`sub` do Keycloak ký và Resource Server xác thực. Repository vẫn query theo
`id AND owner_id`; tài nguyên của user khác trả `404` để không tiết lộ sự tồn tại.

## 4. Gateway behavior

### Routing

| Public path | Downstream |
|---|---|
| `/api/v1/tasks/**` | `${TASK_SERVICE_URI:http://localhost:8081}` |
| `/api/v1/task-lists/**` | Task Service |
| `/api/v1/task-tags/**` | Task Service |
| `/api/v1/files/**` | `${FILE_SERVICE_URI:http://localhost:8082}` |

### Correlation ID

Gateway giữ `X-Correlation-Id` hợp lệ do caller gửi hoặc sinh UUID mới. Giá trị được gửi xuống
service và trả lại response, kể cả response bị Security chặn. Production logging nên thêm giá trị
này vào MDC/structured log để trace xuyên service.

### Timeout và downstream error

- connect timeout mặc định: `2s`;
- response timeout mặc định: `5s`;
- connect failure: `502 Bad Gateway`;
- timeout: `504 Gateway Timeout`;
- response lỗi Gateway dùng `application/problem+json` và chứa `correlationId`.

Gateway không đổi body lỗi hợp lệ do downstream trả về; chỉ chuẩn hóa lỗi hạ tầng phát sinh tại
Gateway.

## 5. Chạy local

Build jar:

```bash
mvn verify
```

Khởi động stack:

```bash
docker compose up --build
```

Địa chỉ:

- Gateway: `http://localhost:8080`;
- Task Service trực tiếp trong Docker Compose: `http://localhost:8181` (container vẫn chạy
  port `8081`; đặt `TASK_SERVICE_HOST_PORT=8081` nếu port đó đang trống);
- Keycloak: `http://localhost:8090`;
- Keycloak Admin Console: user/password `admin` / `admin`;
- Swagger Task Service: `http://localhost:8081/swagger-ui.html`.

Tài khoản demo chỉ dành cho local:

| User | Password | Role | JWT subject / owner ID |
|---|---|---|---|
| `workspace-user` | `workspace-user` | `USER` | `11111111-1111-1111-1111-111111111111` |
| `workspace-admin` | `workspace-admin` | `USER`, `ADMIN` | `22222222-2222-2222-2222-222222222222` |

Không sử dụng các credential này ở production. Production phải dùng secret manager, TLS,
hostname thật và database riêng cho Keycloak.

## 6. Trả lời phần kiến thức trong ticket

### API Gateway, reverse proxy và BFF khác nhau thế nào?

- Reverse proxy chuyển request đến upstream, thường tập trung TLS termination, load balancing
  và network routing.
- API Gateway hiểu API-level concern như authentication, rate limit, correlation, route,
  timeout và observability.
- BFF (Backend for Frontend) cung cấp API được thiết kế riêng cho một loại client, có thể
  aggregate/transform nhiều domain response theo nhu cầu UI.

Gateway hiện tại không phải BFF: nó không aggregate task/file và không chứa presentation logic.

### OAuth2 và OIDC khác nhau thế nào?

OAuth2 là framework authorization: client nhận access token để gọi resource server. OAuth2 tự
thân không định nghĩa “đăng nhập user là ai”. OIDC xây trên OAuth2, thêm identity layer, ID token,
UserInfo và các claim chuẩn như `sub`, `name`, `email`.

Trong hệ thống này, OAuth2 bảo vệ API; OIDC cung cấp login/identity cho SPA.

### JWT, JWKS, issuer và audience là gì?

- JWT là token có claim và chữ ký; payload đọc được nên không đặt secret trong đó.
- JWKS là tập public key để service kiểm tra chữ ký mà không cần chia sẻ private key.
- `iss` xác nhận tổ chức/realm đã phát token.
- `aud` xác nhận token được phát cho API nào.

Chỉ kiểm tra chữ ký là chưa đủ: token hợp lệ của realm khác hoặc dành cho API khác vẫn không được
chấp nhận. Vì vậy implementation kiểm tra cả `iss` và `aud=workspace-api`.

### Authorization Code + PKCE hoạt động ra sao?

Authorization code là mã ngắn hạn dùng một lần. PKCE ràng buộc code với `code_verifier` bí mật
tạm thời do SPA tạo. Kẻ tấn công chặn được authorization code vẫn không đổi token được nếu không
có verifier. PKCE thay thế nhu cầu client secret mà browser không thể giữ an toàn.

### Vì sao chỉ bảo mật Gateway là nguy hiểm?

Service có thể bị gọi trực tiếp qua port nội bộ, SSRF, cấu hình ingress sai hoặc một workload đã
bị compromise. Nếu service tin header do Gateway thêm mà không tự validate token, attacker có
thể bypass toàn bộ authorization. Defense in depth yêu cầu mỗi service là OAuth2 Resource Server.

### CORS và CSRF cho SPA dùng bearer token

CORS là chính sách browser quyết định origin nào được gọi API và đọc response. Gateway chỉ cho
`http://localhost:5173`, các method cần thiết và `Authorization` header.

CSRF chủ yếu khai thác credential được browser tự động gửi như cookie session. Bearer token trong
`Authorization` header không tự được browser gắn vào cross-site request, nên API stateless hiện
tại tắt CSRF. Nếu sau này chuyển token sang cookie, phải bật biện pháp CSRF phù hợp và cấu hình
`SameSite`, `Secure`, domain/origin cẩn thận.

## 7. Acceptance criteria và bằng chứng

| Tiêu chí | Kết quả |
|---|---|
| Login lấy JWT | Realm/client PKCE và user local đã cấu hình |
| Gateway route đúng | Route tests xác nhận Task/File route tồn tại |
| Task Service từ chối token sai khi gọi trực tiếp | Resource Server và tests `401`/`403` |
| File Service từ chối token sai khi gọi trực tiếp | Chưa xác minh vì File Service chưa tồn tại |
| Owner ID lấy từ token subject | Controllers chỉ dùng `Jwt.sub`; header cũ đã xóa |
| Không token = 401 | Automated test |
| Sai role = 403 | Automated test |
| Valid USER = success | HTTP integration tests dùng JWT mock đã xác thực |

## 8. Lựa chọn và giới hạn production

- Keycloak `start-dev`, credential mẫu và HTTP chỉ dành cho local.
- Rate limiting được xác định là trách nhiệm Gateway nhưng chưa có quota/Redis requirement cụ thể
  trong acceptance criteria; không thêm thuật toán/quota tùy ý.
- Gateway không lưu session và không lưu access token.
- Frontend hiện chưa tích hợp OIDC client; đó là bước UI tiếp theo để tự động login/refresh token.
- File Service cần được bổ sung Resource Server trước khi route được dùng thực tế.

## 9. Tệp chính thay đổi

- `api-gateway/`: module Gateway, security, routes, filter, error handler và tests.
- `keycloak/import/personal-workspace-realm.json`: realm có thể import lặp lại.
- `task-service/.../configuration/SecurityConfiguration.java`: Resource Server.
- `task-service/.../security/`: audience validation và mapping authenticated owner.
- `task-service/.../controller/`: bỏ `X-Owner-Id`, dùng JWT `sub`.
- `compose.yml`: Keycloak, PostgreSQL, Task Service và Gateway.
