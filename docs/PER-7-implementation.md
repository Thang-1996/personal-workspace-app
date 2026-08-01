# PER-7 — Thiết kế Task database, JPA model và REST API

Nguồn yêu cầu: [Jira PER-7](https://minhthang009.atlassian.net/browse/PER-7)

## 1. Kết luận đối chiếu code cũ

PER-25 đã cung cấp đúng nền móng và được giữ nguyên: Maven multi-module, Spring Boot,
package `controller/service/repository/entity/dto/exception/configuration`, CRUD Task/List,
JPA/Hibernate, Flyway, H2/PostgreSQL profile, `@Version`, Problem Details, Swagger,
Actuator và Lombok.

PER-7 chỉ bổ sung các phần còn thiếu:

- owner isolation bằng `owner_id`;
- priority, due date, completed time, position, color và archived;
- tag, quan hệ task–tag và schema comment;
- filter động, offset pagination và sort;
- `PATCH /tasks/{id}/status`, `PATCH /task-lists/{id}`;
- Docker Compose PostgreSQL, Flyway PostgreSQL module và Testcontainers;
- MapStruct để tách mapping Entity/DTO khỏi DTO record.

## 2. Công nghệ mới và lý do

### MapStruct 1.6.3

MapStruct sinh mã Java tại compile time, không dùng reflection lúc runtime. Khi response có
owner, priority, deadline, list và tags, factory `TaskResponse.from(entity)` bắt đầu chứa
nhiều mapping thủ công. Package `mapper` bổ sung vẫn nằm giữa service và DTO, không thay đổi
layered architecture.

Không dùng `BeanUtils` hoặc model mapper reflection vì khó kiểm tra field thiếu khi compile.
`lombok-mapstruct-binding` bảo đảm MapStruct nhìn thấy getter do Lombok sinh ra.

### JPA Specification

`TaskSpecifications` ghép predicate chỉ khi filter có giá trị. Một repository duy nhất xử lý
`status`, `priority`, `listId`, `tagId`, `dueFrom`, `dueTo`, `keyword`; không cần tạo hàng
chục derived-query method cho mọi tổ hợp.

Predicate `ownedBy(ownerId)` luôn được ghép trước filter. Vì vậy pagination, count query và
mọi kết quả đều bị scope theo owner.

### Testcontainers PostgreSQL 2.0.5

H2 nhanh và phù hợp phần lớn API test, nhưng không chứng minh SQL migration chạy trên
PostgreSQL thật. `TaskPostgreSqlIT` khởi động `postgres:17-alpine`, chạy Flyway rồi kiểm tra
tables/indexes. Test tự skip nếu Docker không khả dụng; trên CI có Docker, đây là database
integration test thật. Dòng 2.x dùng các module `testcontainers-junit-jupiter` và
`testcontainers-postgresql`; phiên bản này tương thích Docker Engine 29, trong khi 1.21.x
không còn negotiate đúng Docker API mới trên môi trường phát triển hiện tại.

### Flyway PostgreSQL support

Flyway mới tách database-specific support khỏi `flyway-core`. Dependency
`flyway-database-postgresql` là bắt buộc để production migration hoạt động ổn định.
Migration cũ không bị sửa; `V3__expand_task_domain.sql` nâng schema theo nguyên tắc immutable
migration history.

## 3. Data model

### task_lists

`id`, `owner_id`, `name`, `description`, `color`, `position`, `archived`, `version`,
`created_at`, `updated_at`. Unique `(owner_id, name)` cho phép hai owner dùng cùng tên nhưng
một owner không tạo trùng.

### tasks

`id`, `owner_id`, `task_list_id`, `title`, `description`, `status`, `priority`, `due_at`,
`completed_at`, `position`, `version`, `created_at`, `updated_at`.

Khi status thành `DONE`, entity đặt `completed_at`; khi chuyển khỏi DONE, giá trị được xóa.
`@Version` phát hiện update dựa trên phiên bản cũ và ngăn lost update.

### task_tags và task_tag_relations

Tag thuộc owner và unique `(owner_id, name)`. Join table dùng composite primary key
`(task_id, tag_id)`, không cần synthetic id. Service chỉ cho gắn tag cùng owner với task.

### task_comments

Schema/entity đã có để chuẩn bị collaboration, nhưng chưa public API vì PER-7 chỉ yêu cầu
chuẩn bị data model. Binary attachment không nằm trong Task DB; tương lai chỉ lưu reference
tới File Service.

## 4. Ownership tạm thời

Các API đọc/ghi nhận `X-Owner-Id` dạng UUID. Nếu thiếu header, local/dev dùng:

```text
00000000-0000-0000-0000-000000000001
```

Repository không dùng `findById` cho business API mà dùng `findByIdAndOwnerId`. Tài nguyên
của owner khác trả `404`, không trả `403`, tránh tiết lộ ID có tồn tại. JWT ở story ngày 2 sẽ
thay nguồn owner ID, còn service/repository contract không cần đổi.

## 5. API contract

- Task CRUD: `POST/GET/PUT/DELETE /api/v1/tasks`.
- Status: `PATCH /api/v1/tasks/{id}/status`.
- Task List CRUD: `POST/GET/PUT/DELETE /api/v1/task-lists`.
- Partial list update: `PATCH /api/v1/task-lists/{id}`; field null nghĩa là giữ nguyên.
- Tag: `POST/GET /api/v1/task-tags`.
- Pagination: `page` zero-based, `size`, `sort=field,direction`.
- Filter task: `status`, `priority`, `listId`, `tagId`, `dueFrom`, `dueTo`, `keyword`.

Response trang chứa `content`, `totalElements`, `totalPages`, `number` và `size`.

## 6. Giải thích kiến thức trong ticket

### Persistence Context, dirty checking và flush là gì?

Entity được load trong transaction trở thành managed. Khi behavior method đổi field,
Hibernate chụp snapshot và so sánh lúc flush; service không cần gọi `save` sau mỗi update.
Flush biến thay đổi thành SQL trước commit. Transaction boundary đặt ở service để cả load,
validate ownership, resolve list/tag và update cùng một đơn vị nguyên tử.

### LAZY/EAGER và N+1

Quan hệ Task→TaskList và Task↔Tag dùng LAZY để list query không tự tải toàn bộ graph.
Mapper chạy bên trong read-only transaction nên có thể truy cập ID/tag khi cần. N+1 là khi
một query lấy N task rồi phát thêm N query cho quan hệ. Với tải lớn có thể tối ưu bằng
`@EntityGraph`, fetch join hoặc DTO projection sau khi đo query thực tế; không đổi sang EAGER
toàn cục vì EAGER dễ tạo truy vấn thừa và Cartesian product.

### JPA Specification/dynamic query

Specification biểu diễn từng predicate độc lập và ghép bằng `and`. Cách này giữ filter mở
rộng được, tái sử dụng owner predicate và để Spring Data tự sinh cả data query lẫn count
query cho pagination.

### Optimistic locking

`@Version` thêm điều kiện `WHERE id=? AND version=?` khi update. Nếu request khác đã tăng
version, update ảnh hưởng 0 row và Hibernate ném optimistic-lock exception. API đổi thành
Problem Details `409 Conflict`, yêu cầu client tải bản mới thay vì âm thầm ghi đè.

### Unique constraint và application check khác nhau thế nào?

Application check cho message thân thiện trước khi insert. Database unique constraint mới
là bảo vệ cuối cùng trước race condition khi hai request đồng thời cùng thấy “chưa tồn tại”.
PER-7 dùng cả hai cho list/tag theo owner. Production có thể bổ sung mapping
`DataIntegrityViolationException` chi tiết theo constraint name.

### Offset pagination

`page=2&size=20` tương đương bỏ qua 40 bản ghi rồi lấy 20. Ưu điểm là dễ dùng, có total count
và đi thẳng tới page bất kỳ. Nhược điểm là offset lớn chậm và dữ liệu có thể dịch chuyển khi
đang insert. Với volume lớn, có thể chuyển sang cursor/keyset pagination nhưng PER-7 yêu cầu
offset pagination và daily personal task volume phù hợp.

## 7. Cách chạy

```bash
docker compose up -d task-postgres
SPRING_PROFILES_ACTIVE=prod \
DB_URL=jdbc:postgresql://localhost:5432/task_db \
DB_USERNAME=task_service \
DB_PASSWORD=task_service_local \
mvn -pl task-service spring-boot:run
```

DBeaver kết nối `localhost:5432`, database `task_db`, user `task_service`.
Không commit password production; credential trong Compose chỉ dành cho local development.
