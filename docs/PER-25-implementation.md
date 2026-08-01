# PER-25 — Tạo Spring Boot Task Service và dependencies

Nguồn yêu cầu: [Jira PER-25](https://minhthang009.atlassian.net/browse/PER-25)
Ticket cha: [Jira PER-6](https://minhthang009.atlassian.net/browse/PER-6)

## 1. Tóm tắt yêu cầu

PER-25 yêu cầu khởi tạo Task Service đầu tiên cho chức năng task/todo, chạy ở port
`8081`, có Actuator health/info, Swagger/OpenAPI, Bean Validation, package theo feature
và một error response dựa trên Problem Details. Definition of Done gồm:

1. Service khởi động được.
2. `/actuator/health` trả trạng thái `UP`.
3. Swagger UI mở được.
4. Controller không chứa business logic.

Sau yêu cầu refactor bổ sung, service áp dụng architecture thường dùng trong dự án Spring
Boot doanh nghiệp và cung cấp CRUD Task tối thiểu. Boundary được giữ rõ: HTTP controller,
application service, domain entity, Spring Data repository và database.

## 2. Trạng thái code trước khi triển khai

Workspace ban đầu hoàn toàn trống:

- Không có `pom.xml`, source code, test, cấu hình hoặc tài liệu.
- Không có Git repository trong thư mục làm việc.
- Không có code đã triển khai sẵn.
- Không có code đang bị comment-out.
- Máy có JDK 25, nhưng lệnh `mvn` chưa được cài.

Vì không có baseline code, toàn bộ file được liệt kê bên dưới là phần thêm mới cho PER-25
và phần foundation tối thiểu lấy từ conventions của ticket cha PER-6.

## 3. Những thay đổi đã thực hiện

### 3.1 Maven multi-module và dependency

Root `pom.xml` là cả parent POM và aggregator POM:

- Quản lý Spring Boot `3.5.7`, Java release `21` và springdoc `2.8.17`.
- Aggregate module `task-service`, cho phép build từ root.
- Khai báo Failsafe để chạy integration test có hậu tố `*IT`.

`task-service/pom.xml` thêm đúng dependency của ticket:

- `spring-boot-starter-web`: Spring MVC, embedded servlet container và JSON.
- `spring-boot-starter-validation`: Bean Validation cho DTO ở các API sau.
- `spring-boot-starter-actuator`: health/info và nền tảng metrics.
- `spring-boot-starter-data-jpa`: transaction, Spring Data repository và Hibernate ORM.
- `flyway-core`: version hóa schema; Hibernate không tự tạo/sửa bảng.
- `h2`: database chạy độc lập cho local/test.
- `postgresql`: JDBC driver cho production.
- `lombok`: sinh constructor injection, getter và JPA no-args constructor ở compile-time,
  giảm boilerplate nhưng không dùng `@Data` trên entity.
- `springdoc-openapi-starter-webmvc-ui`: sinh OpenAPI và Swagger UI.
- `spring-boot-starter-test`: JUnit 5, AssertJ và Spring Test.

Spring Boot parent đã quản lý version cho các Spring starter và test dependency. Springdoc
không thuộc Spring Boot BOM nên version được khai báo tập trung ở root property.

### 3.2 Application và classic layered package structure

`TaskServiceApplication` nằm ở package gốc để component scan bao phủ:

- `controller`: REST endpoints, HTTP status/header.
- `service`: use case, business validation và transaction boundary.
- `repository`: Spring Data JPA persistence access.
- `entity`: Hibernate/JPA entities và enum.
- `dto/task`, `dto/tasklist`: request/response contracts.
- `exception`: exception classes và Problem Details handler.
- `configuration`: cấu hình kỹ thuật như OpenAPI.

```text
com.personalworkspace.taskservice/
├── controller/
│   ├── TaskController
│   └── TaskListController
├── service/
│   ├── TaskService
│   └── TaskListService
├── repository/
├── entity/
├── dto/
│   ├── task/
│   └── tasklist/
├── exception/
└── configuration/
```

Luồng gọi là `controller → service → repository → Hibernate → database`. Entity không được
trả trực tiếp ra HTTP; repository không được inject vào controller.

### 3.3 Controller, service và persistence boundary

| Method | Path | Chức năng |
|---|---|---|
| `POST` | `/api/v1/tasks` | Tạo task, trả `201` và `Location` |
| `GET` | `/api/v1/tasks/{id}` | Lấy task |
| `GET` | `/api/v1/tasks?status=TODO` | Danh sách, tùy chọn lọc status |
| `PUT` | `/api/v1/tasks/{id}` | Cập nhật nội dung và status |
| `DELETE` | `/api/v1/tasks/{id}` | Xóa task, trả `204` |

Task List API:

| Method | Path | Chức năng |
|---|---|---|
| `POST` | `/api/v1/task-lists` | Tạo task list |
| `GET` | `/api/v1/task-lists` | Danh sách task list |
| `GET` | `/api/v1/task-lists/{id}` | Chi tiết task list |
| `GET` | `/api/v1/task-lists/{id}/tasks` | Các task thuộc list |
| `PUT` | `/api/v1/task-lists/{id}` | Cập nhật task list |
| `DELETE` | `/api/v1/task-lists/{id}` | Xóa list rỗng |

Controller chỉ xử lý HTTP. `TaskService` là transaction boundary; `TaskRepository` kế thừa
`JpaRepository` và không được controller dùng trực tiếp. DTO immutable tách JSON contract
khỏi JPA entity.

Lombok `@RequiredArgsConstructor` tạo constructor cho dependency `final` ở controller/service.
Entity dùng `@Getter` và `@NoArgsConstructor(PROTECTED)`. Không dùng `@Data` vì generated
`equals/hashCode/toString` trên JPA relationship có thể gây lazy loading hoặc recursion.

Entity `Task` và `TaskList` dùng UUID, timestamp UTC và `@Version` optimistic locking.
`Task` có quan hệ `ManyToOne` đến `TaskList`; task có thể chưa thuộc list.
Invariant title nằm trong entity. Flyway migration tạo bảng/index; `ddl-auto=validate` làm
application fail-fast nếu mapping và schema lệch. H2 mặc định chạy PostgreSQL compatibility
mode; profile prod lấy kết nối PostgreSQL từ environment.

### 3.4 Actuator và externalized configuration

`application.yml`:

- Port mặc định `8081`, có thể override bằng `SERVER_PORT`.
- Chỉ expose `health` và `info`; không dùng wildcard vì có thể làm lộ endpoint quản trị.
- Health production không hiển thị chi tiết component.
- Info cung cấp tên, mô tả và version ứng dụng.

Ba profile `local`, `test`, `prod` đã được tạo. `local` cho phép xem health detail;
`test` dùng random port; `prod` đọc port từ môi trường và ẩn health detail. Không có secret
thật trong repository.

### 3.5 OpenAPI và Swagger

`OpenApiConfiguration` tạo một bean `OpenAPI` có:

- Title: `Personal Workspace Task Service API`.
- Version API: `v1`.
- Mô tả phạm vi Task Service.

Swagger UI ở `/swagger-ui.html`; OpenAPI JSON ở `/v3/api-docs`.

Swagger UI được cấu hình để thử API trực tiếp bằng **Try it out**, sắp xếp endpoint theo
HTTP method, sắp xếp nhóm theo tên và hiển thị thời gian request. Hai nhóm nghiệp vụ là
`Tasks` và `Task Lists`; từng operation có mô tả, HTTP response code, validation schema và
JSON example. Khi chạy local, mở `http://localhost:8081/swagger-ui.html`, chọn endpoint,
nhấn **Try it out**, điền request mẫu rồi nhấn **Execute**.

### 3.6 Problem Details

`GlobalExceptionHandler` kế thừa `ResponseEntityExceptionHandler` và dùng
`@ControllerAdvice`, tạo error response tập trung theo RFC 9457. Lỗi Bean Validation trả:

```json
{
  "type": "https://personal-workspace.example/problems/validation-error",
  "title": "Validation failed",
  "status": 400,
  "detail": "Dữ liệu request không hợp lệ.",
  "instance": "/api/example",
  "errors": [
    {"field": "title", "message": "không được để trống"}
  ]
}
```

`type` là URI ổn định để client phân loại lỗi; `detail` dành cho con người; `errors` cung
cấp lỗi từng field. Handler sẽ thực sự được dùng khi các request DTO có `@Valid` được thêm.

### 3.7 Test

- `TaskServiceApplicationTests`: xác nhận context khởi động và bean OpenAPI đúng metadata.
- `TaskServiceEndpointsIT`: xác nhận health, OpenAPI, Swagger, create/read persistence và
  validation Problem Details qua HTTP thật.

## 4. Code comment-out

Không phát hiện code comment-out vì workspace không có code trước khi triển khai. Trong
implementation mới cũng không giữ block code bị comment-out. Các comment/Javadoc hiện có
đều giải thích quyết định thiết kế hoặc lý do cấu hình, không phải code bị vô hiệu hóa.

Giữ code cũ bằng comment thường gây nhầm lẫn và nhanh lỗi thời; lịch sử thay đổi nên được
quản lý bằng Git. Phần chưa làm được ghi rõ trong tài liệu/backlog, không giữ skeleton
business logic bị comment.

## 5. Giải đáp kiến thức trong ticket

### 5.1 `@SpringBootApplication` là gì?

Đây là annotation tiện ích kết hợp ba annotation:

1. `@SpringBootConfiguration`: đánh dấu nguồn cấu hình chính của Spring Boot, bản chất là
   một dạng chuyên biệt của `@Configuration`.
2. `@EnableAutoConfiguration`: cho phép Boot cấu hình bean dựa vào dependency trên
   classpath, bean hiện có và property. Ví dụ khi có starter web, Boot cấu hình MVC và
   embedded server.
3. `@ComponentScan`: tìm component từ package của application class trở xuống.

Vì phạm vi scan phụ thuộc vị trí package, application class nên nằm ở package gốc. Auto
configuration là có điều kiện, không phải “ma thuật”: cấu hình người dùng thường có thể
override bean hoặc property mặc định.

### 5.2 IoC, DI và Bean lifecycle

**Inversion of Control (IoC)** nghĩa là application không tự tạo và nối toàn bộ object;
container quản lý việc đó. **Dependency Injection (DI)** là cách container đưa dependency
vào object.

Quy ước dự án là constructor injection vì:

- Dependency bắt buộc được thể hiện rõ trong constructor.
- Field có thể để `final`.
- Unit test tạo object dễ dàng mà không cần reflection hoặc Spring context.
- Tránh object tồn tại ở trạng thái thiếu dependency.

Lifecycle rút gọn của một singleton bean:

1. Spring đọc configuration/component metadata.
2. Tạo `BeanDefinition`.
3. Instantiate bean.
4. Resolve và inject dependency.
5. Chạy các callback “aware” và `BeanPostProcessor` trước initialization.
6. Chạy `@PostConstruct`/initialization callback.
7. Chạy post-processor sau initialization; proxy AOP có thể được tạo ở đây.
8. Bean sẵn sàng phục vụ.
9. Khi context đóng, chạy `@PreDestroy`/destroy callback.

Không nên đặt network call hoặc nghiệp vụ nặng trong constructor. Nếu cần initialization,
hãy dùng lifecycle hook có chủ đích và làm rõ failure behavior.

### 5.3 Request đi qua `DispatcherServlet` như thế nào?

Luồng Spring MVC điển hình:

1. Embedded server nhận HTTP request và chuyển qua servlet filters.
2. `DispatcherServlet` là front controller nhận request.
3. `HandlerMapping` tìm controller method phù hợp.
4. `HandlerAdapter` gọi method; argument resolver đọc path/query/header/body.
5. `HttpMessageConverter` chuyển JSON thành DTO.
6. `@Valid` kích hoạt Bean Validation trước khi controller chạy.
7. Controller ủy quyền use case cho application/service layer.
8. Kết quả được message converter serialize thành JSON.
9. Nếu lỗi xảy ra, exception resolver và `@ControllerAdvice` tạo Problem Details.
10. Response đi ngược qua filter và servlet container về client.

Do controller là HTTP adapter, nó nên chỉ parse/validate input, gọi service và map output;
business rule thuộc service/domain.

### 5.4 Spring Profile và externalized configuration

**Profile** bật một nhóm bean/property theo môi trường, ví dụ `local`, `test`, `prod`.
Profile không nên biến thành ba codebase khác nhau; code giữ nguyên, chỉ cấu hình thay đổi.

**Externalized configuration** cho phép cùng một artifact nhận cấu hình từ nhiều nguồn như
YAML, environment variable, JVM system property và command-line argument. Nguồn có độ ưu
tiên cao override nguồn thấp. Ví dụ:

```bash
SERVER_PORT=9091 SPRING_PROFILES_ACTIVE=prod \
  java -jar task-service.jar
```

Nguyên tắc:

- Commit default không nhạy cảm và `.env.example`.
- Không commit password/token.
- Secret production đến từ secret manager hoặc môi trường runtime.
- Dùng placeholder như `${SERVER_PORT:8081}` để có default an toàn.
- Test nên kích hoạt profile riêng để không phụ thuộc cấu hình máy cá nhân.

## 6. Đối chiếu Definition of Done

| Tiêu chí | Implementation | Cách xác minh |
|---|---|---|
| Service start được | `TaskServiceApplication`, executable Boot JAR | `mvn -pl task-service spring-boot:run` |
| Health trả `UP` | Actuator + exposure `health` | GET `/actuator/health` |
| Swagger mở được | springdoc starter + OpenAPI bean | GET `/swagger-ui.html` |
| Không có business logic trong controller | Controller chỉ bind/validate/delegate; transaction, persistence và rule nằm ở service/domain | Review package/source |

## 7. Phạm vi chưa triển khai

- Authentication/authorization.
- Docker healthcheck và monitoring backend.
- Pagination/sorting nâng cao và production Testcontainers.

Các phần này không có acceptance criteria hoặc contract trong PER-25 và nên được thực hiện
ở ticket tiếp theo.
