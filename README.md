# Personal Workspace Platform

Backend multi-module cho Personal Workspace.

## Service ownership

| Module | Trách nhiệm | Port |
|---|---|---:|
| `task-service` | Sở hữu task và task list; hiện cung cấp service foundation, health và OpenAPI | 8081 |

## Chạy và kiểm tra

Yêu cầu Java 21+ và Maven 3.9+:

```bash
mvn clean verify
mvn -pl task-service spring-boot:run
```

Sau khi service khởi động:

- Health: <http://localhost:8081/actuator/health>
- Info: <http://localhost:8081/actuator/info>
- Swagger UI: <http://localhost:8081/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8081/v3/api-docs>
- Task API: <http://localhost:8081/api/v1/tasks>
- Task List API: <http://localhost:8081/api/v1/task-lists>

Mặc định service dùng H2 in-memory. Profile `prod` dùng PostgreSQL qua `DB_URL`,
`DB_USERNAME`, `DB_PASSWORD`; Flyway quản lý schema ở cả hai môi trường.

Xem tài liệu triển khai và kiến thức của PER-25 tại
[`docs/PER-25-implementation.md`](docs/PER-25-implementation.md).
