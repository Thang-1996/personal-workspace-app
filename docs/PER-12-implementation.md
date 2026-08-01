# PER-12 — Keycloak Auth và Files Manager UI

## 1. Phần đã có và nguyên tắc refactor

PER-8/PER-9 đã có React 19 + TypeScript, feature-based folders, router, Axios,
TanStack Query, Zustand UI state, shared UI và lazy route. PER-12 không dựng lại
project hoặc thêm state framework khác. Thay đổi chỉ bổ sung:

- `features/auth`: Keycloak adapter, auth context và protected route.
- `features/files`: typed model, API/query hooks, dropzone và Files page.
- Bearer interceptor vào `shared/api/httpClient`.
- Files route thật thay placeholder; Vite `/api` proxy sang Gateway port 8080.

## 2. Authentication đã triển khai

`keycloak-js` dùng public client `workspace-web`, Authorization Code Flow và
PKCE S256. Không có client secret trong browser.

`AuthProvider`:

- Chạy `check-sso` khi bootstrap.
- Đưa access token vào Axios request interceptor.
- Refresh token trước khi hết hạn và xử lý `onTokenExpired`.
- Expose `authenticated`, user profile, roles, `hasRole`, `login`, `logout`.
- Logout xóa token và `queryClient.clear()` để dữ liệu owner cũ không còn trong
  memory cache nếu user khác đăng nhập trên cùng tab.

`ProtectedRoute` chỉ render AppShell sau khi auth init và đã authenticated. Khi
chưa login, UI hiển thị nút chuyển đến Keycloak. Backend/Gateway vẫn là nơi quyết
định authorization; frontend role helper chỉ phục vụ UX.

Các biến môi trường:

```text
VITE_API_BASE_URL=/api
VITE_KEYCLOAK_URL=http://localhost:8090
VITE_KEYCLOAK_REALM=personal-workspace
VITE_KEYCLOAK_CLIENT_ID=workspace-web
```

## 3. Files Manager đã triển khai

- Lazy-loaded `/files` route và navigation sẵn có ở Sidebar.
- List folders/files theo folder hiện tại.
- Breadcrumb quay root/ancestor mà không gọi thêm API.
- Tạo folder.
- Native drag/drop và file input keyboard-accessible.
- Upload progress qua Axios `onUploadProgress`; disable thao tác khi đang upload.
- Download Blob qua authenticated Axios, tạo object URL tạm, click anchor rồi
  revoke URL.
- Delete confirmation và targeted cache update.
- Attach file vào Task bằng external task UUID từ Files page hoặc chọn root file
  trực tiếp trong Task Detail Drawer. Query file chỉ bật khi drawer đang mở, tránh
  request nền không liên quan.
- Hiển thị kích thước file/date và loading, empty, API error, toast feedback.

Mutation không invalidate toàn bộ workspace. Upload/create/delete chỉ cập nhật
đúng query cache của folder đang mở; vì vậy không tạo hàng loạt refetch giống vấn
đề đã phân tích ở PER-9.

## 4. Trả lời toàn bộ câu hỏi kỹ thuật

### Authorization Code + PKCE lifecycle hoạt động thế nào?

Browser tạo `code_verifier` ngẫu nhiên và gửi hash `code_challenge` khi redirect
đến Keycloak. Sau login, Keycloak trả authorization code về redirect URI. Adapter
đổi code + verifier lấy token; kẻ chặn được code không có verifier nên không đổi
được token. Access token ngắn hạn dùng gọi API, refresh token dùng gia hạn phiên.
Public SPA không thể giữ client secret an toàn, nên PKCE S256 là bắt buộc.

### Theo dõi upload progress thế nào?

Axios browser adapter nhận progress event từ `XMLHttpRequest` và expose
`loaded/total`. UI tính phần trăm, render progressbar và khóa chọn file kế tiếp.
Progress chỉ mô tả bytes client đã gửi, không đảm bảo server/MinIO commit; chỉ
hiển thị success khi API trả metadata `READY`. Với presigned multipart lớn, cần
tổng hợp progress của từng part và hỗ trợ abort/retry.

### Download Blob cần lưu ý gì?

Request phải đặt `responseType: 'blob'`, sau đó tạo `URL.createObjectURL`, gán cho
anchor có thuộc tính `download`, click và `URL.revokeObjectURL`. Revoke tránh leak
memory. File rất lớn qua Blob vẫn tốn memory browser; production có thể dùng
presigned URL hoặc streaming download sau khi backend đã kiểm tra owner.

### Drag/drop accessible cần gì?

Không được chỉ dựa vào gesture chuột. UI vẫn có `<input type="file">` và button
native có thể focus/Enter/Space; dropzone chỉ là enhancement. Text mô tả loại và
size file, progress dùng `role="status"`, lỗi được thông báo qua live toast.

### TanStack Query invalidation nên làm thế nào?

Query key phải mô tả resource và folder: `['files','list',folderId]`. Mutation đã
có response đầy đủ thì ghi trực tiếp vào đúng cache; delete lọc đúng ID. Chỉ
invalidate/refetch khi client không thể suy ra state server. Không invalidate
prefix `['files']` hoặc toàn QueryClient sau mỗi upload vì sẽ gọi lại mọi folder.
Logout là ngoại lệ: phải clear toàn cache để chống data leakage giữa identities.

### Frontend auth và backend auth khác nhau?

ProtectedRoute/role helper chỉ ngăn UI không phù hợp và cải thiện UX; user có thể
sửa JavaScript hoặc gọi API trực tiếp. Gateway và mỗi Resource Server phải verify
signature, issuer, audience, expiry, role; service còn scope query bằng JWT `sub`.
Backend là security boundary, frontend không bao giờ là nguồn authority.

## 5. Error handling

Axios response interceptor chuẩn hóa Spring Problem Details thành `ApiError`.
Files page hiển thị `detail/title` qua toast/error state. Server vẫn detect MIME
và kiểm tra owner; `accept` trên input chỉ là gợi ý UX, không phải validation bảo
mật.

## 6. Kiểm thử

Quality gate:

```bash
cd frontend-app
npm run check
```

Kết quả hiện tại:

- Oxlint: pass.
- Vitest: 4 files, 5 tests pass.
- TypeScript project build: pass.
- Vite production build: pass, Files page tạo lazy chunk riêng.

`npm audit --omit=dev` hiện còn advisory high cho React Router RSC Mode
(`GHSA-qwww-vcr4-c8h2`). Project là browser-only SPA, không bật RSC, server
actions, SSR hay React Router framework mode nên code path bị ảnh hưởng không
được sử dụng. Đã nâng lên `react-router-dom 7.18.2` để nhận các bản vá khác; npm
hiện không có release đồng thời nằm ngoài advisory RSC này mà không downgrade về
7.11.0 (bản bị nhiều advisory XSS/open redirect khác). Cần tiếp tục theo dõi và
nâng ngay khi upstream phát hành bản fix phù hợp.

## 7. Các cải tiến tiếp theo

- Focus trap đầy đủ cho shared Modal.
- Task picker có search thay vì nhập UUID khi Task API hỗ trợ lightweight lookup.
- AbortController/cancel upload và retry multipart.
- Presigned URL cho file lớn.
- E2E Playwright với Keycloak thật trong CI.
