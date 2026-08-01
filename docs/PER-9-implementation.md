# PER-9 — Dashboard, Task List và Task Detail tích hợp API

## 1. Phạm vi đã có từ PER-8

PER-8 đã tạo đúng nền tảng nên PER-9 không dựng lại project:

- Vite, React, TypeScript, Tailwind và Flowbite.
- Feature-based architecture: `app`, `features`, `shared`.
- Router, QueryClient, ErrorBoundary, Toast provider.
- Axios instance, Zustand UI store, React Hook Form, Zod.
- Responsive AppShell, Sidebar, Topbar và các UI primitives.
- Dashboard preview, TaskCard và create modal ở mức demo.

PER-9 giữ nguyên kiến trúc trên và thay preview workflow bằng tích hợp thật với Task Service.

## 2. Phần bổ sung

### API contract

- Typed Axios functions cho list/detail/create/update/delete/change-status.
- Typed model khớp OpenAPI backend: Task, TaskList, Page metadata, filter và payload.
- Chuẩn hóa RFC 9457/Spring `ProblemDetail` thành `ApiError`.
- Vite proxy `/api` sang `http://localhost:8081`; không cần CORS trong local development.

### TanStack Query

- Query key factory tách task list, task detail và task lists.
- Hooks: `useTasks`, `useTask`, `useTaskLists`, `useCreateTask`, `useUpdateTask`,
  `useDeleteTask`, `useChangeStatus`, `useDashboardStats`.
- Mutation thành công cập nhật detail cache và invalidate đúng list prefix.
- Toggle Done dùng optimistic update; lưu snapshot, rollback khi lỗi và refetch sau cùng.

### UI workflow

- Dashboard metric lấy từ API: Open, Due today, Overdue, Completed.
- Task list có loading skeleton, empty state, API error và pagination.
- Status, priority, task-list filter và keyword search.
- Filters/page được lưu trong URL query string, nên refresh/back/forward và share URL hoạt động.
- Keyword được debounce 350 ms trước khi cập nhật URL và gọi API.
- Create/Edit dùng chung `TaskForm`; tránh hai schema và hai behavior khác nhau.
- Detail drawer mở theo `selectedTaskId` trong Zustand, fetch detail độc lập, hỗ trợ edit/delete.
- Toggle status optimistic và thông báo rollback rõ ràng khi backend lỗi.

## 3. Luồng dữ liệu

```text
URLSearchParams
      │
      ▼
TaskFilters ──► query key ──► GET /api/v1/tasks
      │                          │
      │                          ▼
      └────────────────────► Query cache ──► Task list UI

Toggle Done
  ├─ onMutate: cancel query + snapshot + update cache
  ├─ API PATCH /tasks/{id}/status
  ├─ onError: restore snapshot
  └─ onSettled: invalidate/refetch
```

## 4. Trả lời câu hỏi kỹ thuật

### Mutation lifecycle của TanStack Query hoạt động thế nào?

`mutationFn` thực hiện side effect. `onMutate` chạy trước request và có thể trả về context.
`onError` nhận context để phục hồi; `onSuccess` ghi response chuẩn từ server; `onSettled`
luôn chạy để invalidate/refetch. Tách lifecycle này giúp optimistic update có transaction-like
boundary rõ ràng.

### Optimistic update và rollback nên dùng khi nào?

Chỉ dùng khi thao tác đơn giản, xác suất thành công cao và trạng thái kế tiếp dự đoán được.
Toggle Done đáp ứng điều đó. Create/delete/edit nhiều field không optimistic trong story này
vì server có validation, ownership và optimistic locking; chờ response giúp tránh UI hiển thị
entity giả hoặc version sai.

Trước optimistic update phải:

1. Cancel query đang chạy để response cũ không ghi đè.
2. Snapshot mọi cache entry sẽ thay đổi.
3. Update cache đồng bộ.
4. Rollback toàn bộ snapshot khi lỗi.
5. Invalidate sau cùng để server tiếp tục là nguồn sự thật.

### Vì sao filter nên nằm trong URL?

Filter là navigation state: người dùng mong refresh, back/forward và copy link vẫn giữ kết quả.
`URLSearchParams` đáp ứng các hành vi này và làm query key có thể tái tạo. Modal open hoặc draft
form là ephemeral UI state nên không cần đưa vào URL.

### Controlled và uncontrolled form khác nhau?

Controlled input nhận `value/onChange` từ React ở mỗi lần gõ; phù hợp search box cần debounce và
đồng bộ URL. React Hook Form mặc định dùng uncontrolled ref cho form nhiều field, giảm re-render.
`reset` được dùng khi task detail mới tải về để đồng bộ default values của edit form.

### Làm sao tránh render không cần thiết?

- Query key chỉ chứa filter có ý nghĩa.
- `useMemo` tạo map TaskList theo id thay vì tìm tuyến tính cho từng card.
- `useCallback` giữ handler ổn định; `TaskCard` dùng `memo`.
- Debounce search tránh request/render trên từng phím.
- Detail chỉ fetch khi có selected id.
- Route Dashboard tiếp tục được lazy-load từ PER-8.

Không nên `useMemo/useCallback` mọi nơi; chỉ dùng ở boundary có component memoized, dữ liệu dẫn
xuất đáng kể hoặc dependency stability ảnh hưởng side effect.

### Drawer/modal accessible cần gì?

- `role="dialog"`, `aria-modal`, accessible name.
- Khi mở chuyển focus vào drawer; Escape đóng; khi đóng trả focus về trigger trước đó.
- Có nút đóng thật và overlay click.
- Form control có label; action dùng button native.

Drawer hiện đáp ứng các điểm trên. Với UI phức tạp hơn nên bổ sung focus trap hoàn chỉnh bằng
headless primitive đã được audit, thay vì tự mở rộng keyboard logic rải rác.

### Problem Details normalization mang lại gì?

Backend trả cấu trúc chuẩn gồm `status`, `title`, `detail`, `type`, `instance`. Interceptor chuyển
nó thành `ApiError`, để component không phụ thuộc Axios shape và luôn hiển thị `detail/title`
nhất quán. HTTP status vẫn được giữ cho xử lý chuyên biệt 400/404/409 khi cần.

## 5. Dashboard metric

- Open = tổng TODO + IN_PROGRESS.
- Due today = TODO và IN_PROGRESS có `dueAt` trong ngày hiện tại.
- Overdue = TODO và IN_PROGRESS có `dueAt` trước thời điểm hiện tại.
- Completed = tổng DONE.

Mỗi metric dùng metadata `totalElements`, không đếm riêng page hiện tại nên vẫn chính xác khi có
nhiều trang.

## 6. Kiểm thử

- Form validation không submit title quá ngắn.
- TaskCard toggle gửi trạng thái DONE.
- Optimistic mutation đổi cache ngay và phục hồi TODO khi API lỗi.
- Full quality gate: lint, Vitest và production build.
- Integration smoke test đi qua Vite proxy tới backend port 8081.

