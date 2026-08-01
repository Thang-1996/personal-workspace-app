# PER-11 — File Service, PostgreSQL metadata và MinIO

## 1. Phạm vi đã triển khai

Story bổ sung module Maven `file-service` (port `8082`) theo cấu trúc hiện tại của
backend: `controller`, `service`, `repository`, `entity`, `dto`, `exception`,
`configuration` và `storage`. Service dùng:

- Spring Web/Validation/Actuator cho REST API và health check.
- Spring Security OAuth2 Resource Server để xác thực JWT Keycloak.
- Spring Data JPA/Hibernate và Flyway để quản lý metadata.
- PostgreSQL `file_db` trong Docker; H2 PostgreSQL mode cho integration test nhanh.
- MinIO SDK để lưu binary trong bucket `workspace-files`.
- Apache Tika để nhận diện MIME từ nội dung thay vì tin header/đuôi file.
- Lombok chỉ ở entity/service để giảm constructor/getter boilerplate.

Gateway đã route cả `/api/v1/files/**` và `/api/v1/folders/**` đến File Service.
Docker Compose bổ sung PostgreSQL riêng, MinIO và File Service; không chia sẻ
database với Task Service.

File Service phát domain event `FileUploaded` và `FileDeleted` trong transaction.
Listener có thể dùng `@TransactionalEventListener(AFTER_COMMIT)` để chỉ xử lý khi
metadata commit thành công. Khi cần giao tiếp cross-service đáng tin cậy, chuyển
event này sang transactional outbox/message broker.

## 2. Data model

### `folders`

Lưu `id`, `owner_id`, `parent_id`, `name`, `created_at`, `updated_at`. Quan hệ
self-reference tạo cây thư mục. Service luôn truy vấn bằng cả `id` và `owner_id`;
do đó một user không thể dùng UUID đoán được để đi vào cây của user khác.

### `files`

Lưu metadata: tên gốc, random storage key, MIME đã detect, kích thước, SHA-256,
folder, trạng thái và timestamps. Binary không nằm trong PostgreSQL.

Trạng thái:

- `PENDING`: metadata đã bắt đầu tạo, object chưa được xác nhận.
- `READY`: object upload thành công, file có thể list/download/link.
- `DELETED`: soft-delete metadata sau khi object đã xóa.

### `file_links`

Liên kết `(file_id, linked_entity_type, linked_entity_id)`. Task ID là external
UUID, không tạo foreign key cross-service. Unique constraint làm thao tác attach
idempotent.

## 3. API đã triển khai

| Method | Endpoint | Ý nghĩa |
|---|---|---|
| POST | `/api/v1/files/upload` | Multipart upload, tùy chọn `folderId` |
| GET | `/api/v1/files` | List theo owner; filter `folderId`, `name`, `type` |
| GET | `/api/v1/files/{id}/download` | Stream file với content type và attachment filename |
| DELETE | `/api/v1/files/{id}` | Xóa object rồi soft-delete metadata |
| POST | `/api/v1/files/{id}/links/tasks/{taskId}` | Attach file vào Task bằng external ID |
| POST | `/api/v1/folders` | Tạo folder |
| GET | `/api/v1/folders` | List folder theo `parentId` |
| PATCH | `/api/v1/folders/{id}` | Đổi tên folder |

Mọi API nghiệp vụ yêu cầu role `USER` hoặc `ADMIN`. `owner_id` lấy từ JWT `sub`,
không nhận từ request, tránh horizontal privilege escalation.

## 4. Luồng upload và consistency

1. Kiểm tra file không rỗng và không vượt `FILE_MAX_SIZE_BYTES`.
2. Đọc bytes, dùng Tika detect MIME, so với allowlist.
3. Chuẩn hóa tên hiển thị; storage key là `{ownerId}/{random UUID}` nên không có
   collision hoặc path traversal từ filename.
4. Tính checksum SHA-256.
5. Save metadata `PENDING`.
6. Upload object vào MinIO.
7. Mark metadata `READY`.
8. Nếu upload lỗi, cố gắng xóa object và rollback transaction metadata.

Delete làm theo thứ tự object trước, metadata sau. Nếu MinIO lỗi, metadata vẫn
`READY` để có thể retry, thay vì báo đã xóa trong khi object còn tồn tại. Với yêu
cầu production cao hơn, nên dùng outbox + background reconciler để xử lý trường
hợp hiếm DB commit lỗi sau khi object đã xóa.

## 5. Các quyết định bảo mật

- Không dùng filename làm object key.
- Không expose access key/secret key hoặc đường dẫn MinIO cho browser.
- MIME được detect server-side; `Content-Type` client chỉ là gợi ý.
- Owner scope được đặt trong mọi query read/write.
- Download dùng `Content-Disposition: attachment` để hạn chế browser render nội
  dung nguy hiểm.
- Error trả Problem Details và không làm lộ storage credentials/internal key.

## 6. Trả lời toàn bộ câu hỏi kiến thức trong ticket

### Object storage, filesystem và database BLOB khác nhau thế nào?

Filesystem local đơn giản nhưng gắn dữ liệu với một server, khó scale ngang và
backup/replicate nhất quán. BLOB giữ transaction cùng metadata nhưng làm database
phình nhanh, backup chậm và tốn connection/bandwidth DB. Object storage tách
binary khỏi compute/database, scale tốt, có lifecycle/versioning/S3 API; đổi lại
metadata và object không có một ACID transaction chung. Kiến trúc này chọn
PostgreSQL cho metadata/query và MinIO cho binary.

### Multipart upload là gì?

HTTP `multipart/form-data` cho phép gửi file và field metadata trong một request.
S3 multipart upload còn có nghĩa khác: chia object lớn thành nhiều part, upload
song song và complete bằng upload ID. API hiện dùng HTTP multipart và MinIO SDK
single-object stream; với file rất lớn nên chuyển sang S3 multipart/presigned
parts để retry từng phần và không giữ toàn bộ bytes trong RAM.

### Presigned URL là gì và khi nào dùng?

Đó là URL có chữ ký, quyền và thời hạn ngắn để client upload/download trực tiếp
object mà không nhận storage credentials. Nó giảm tải bandwidth cho backend.
Backend vẫn phải kiểm tra owner trước khi phát URL, giới hạn expiry, method,
content length/type. Bản hiện tại stream qua service để ownership và audit đơn
giản; presigned URL là tối ưu hóa tiếp theo.

### MIME spoofing là gì?

Client có thể đổi `.exe` thành `.pdf` hoặc gửi header `application/pdf`. Vì vậy
không được tin extension/header. File Service dùng Apache Tika đọc magic
bytes/content signature rồi đối chiếu allowlist. Với hệ thống production còn nên
quét antivirus, sandbox file nguy hiểm và đặt `X-Content-Type-Options: nosniff`.

### Path traversal là gì?

Tên như `../../secret` có thể thoát khỏi thư mục đích nếu ghép trực tiếp vào
filesystem path. Object storage không có directory thật nhưng key độc hại vẫn gây
nhầm namespace/policy. Code chỉ giữ basename làm tên hiển thị và tạo storage key
từ owner UUID + random UUID, nên input không quyết định storage path.

### Compensating cleanup là gì?

PostgreSQL và MinIO không cùng một distributed transaction. Khi bước sau thất
bại, hệ thống thực hiện hành động bù cho bước trước: upload lỗi thì delete object
nếu đã được tạo và rollback metadata. Production nên có thêm scheduled
reconciler quét `PENDING` quá hạn/orphan để xử lý cả trường hợp process crash.

### S3 consistency và lifecycle cần hiểu gì?

S3 hiện cung cấp strong read-after-write/list consistency cho object operations;
nhưng application vẫn có inconsistency vì database và storage commit riêng.
Lifecycle rule tự chuyển tier hoặc xóa object theo age/tag, hữu ích cho trash,
retention và cost. Không nên đặt lifecycle xóa object sớm hơn retention metadata;
soft-deleted object nên có policy rõ ràng và audit/recovery window.

## 7. Kiểm thử và bằng chứng

`FileServiceEndpointsTest` chạy Spring Boot thật với Flyway, Hibernate validate,
security filter chain và H2; chỉ mock biên MinIO/JWT decoder:

- Public health, API 401 khi thiếu token, 403 khi thiếu role.
- Upload → download → attach Task → delete.
- Owner B không thể download file của owner A.
- Folder/file list được scope theo owner.
- File giả MIME bị từ chối và không tạo object.

Kết quả: **4 tests, 0 failures, 0 errors**.

Lệnh kiểm tra:

```bash
mvn -pl file-service test
docker compose config
docker compose up -d --build file-postgres minio file-service api-gateway
```

## 8. Phần mở rộng chưa nằm trong acceptance hiện tại

- S3 multipart/presigned upload cho file lớn.
- Antivirus scanning và quarantine workflow.
- Transactional outbox/message broker cho `FileUploaded`/`FileDeleted` cross-service.
- Reconciler định kỳ cho `PENDING` quá hạn và orphan object.
- Pagination database-side thay vì in-memory filtering.

Đây là các hướng production-hardening; không cần thêm framework/abstraction vào
story hiện tại khi chưa có consumer hoặc tải thực tế.
