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
- Public API Gateway: <http://localhost:8080>
- Task API trực tiếp: <http://localhost:8081/api/v1/tasks>
- Keycloak: <http://localhost:8090>
- Task List API: <http://localhost:8081/api/v1/task-lists>

Mặc định service dùng H2 in-memory. Chạy PostgreSQL local bằng:

```bash
docker compose up -d task-postgres
SPRING_PROFILES_ACTIVE=prod \
DB_URL=jdbc:postgresql://localhost:5432/task_db \
DB_USERNAME=task_service \
DB_PASSWORD=task_service_local \
mvn -pl task-service spring-boot:run
```

Flyway quản lý schema ở cả H2 và PostgreSQL. Từ PER-10, API yêu cầu Bearer JWT hợp lệ;
`owner_id` được lấy từ claim `sub`, không còn tin header `X-Owner-Id`.

Chi tiết Gateway, Keycloak, tài khoản local, cấu hình và phần kiến thức:
[docs/PER-10-implementation.md](docs/PER-10-implementation.md).
Danh sách task hỗ trợ filter, pagination và sort:

```text
GET /api/v1/tasks?status=TODO&priority=HIGH&listId=...&tagId=...
    &dueFrom=2026-08-01T00:00:00Z&dueTo=2026-08-31T23:59:59Z
    &keyword=postgres&page=0&size=20&sort=dueAt,asc
```

Xem tài liệu triển khai và kiến thức của PER-25 tại
[`docs/PER-25-implementation.md`](docs/PER-25-implementation.md).
Tài liệu thiết kế domain, database và kiến thức của PER-7:
[`docs/PER-7-implementation.md`](docs/PER-7-implementation.md).

## Frontend

React/Vite UI nằm tại `frontend-app`:

```bash
cd frontend-app
npm install
npm run dev
```

Vite chạy tại <http://localhost:5173> và proxy `/api` sang Task Service ở port
`8081`. Chạy `npm run check` để lint, test và production build. Xem kiến trúc,
UI reference, screen list và giải đáp kỹ thuật tại
[`docs/PER-8-implementation.md`](docs/PER-8-implementation.md).

Dashboard, Task List, Task Detail, CRUD, filter/pagination và optimistic update
được mô tả tại [`docs/PER-9-implementation.md`](docs/PER-9-implementation.md).
