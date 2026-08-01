# PER-8 — Setup React Project

## 1. Kết quả triển khai

Đã tạo `frontend-app` bằng React, TypeScript và Vite. Project không dừng ở scaffold mặc định mà có composition root, providers, router, responsive app shell, feature module Task, shared UI primitives, validation và test.

## 2. Kiến trúc enterprise được chọn

```text
src/
├── app/                    # composition root, provider, router, shell, global UI store
├── features/
│   ├── tasks/              # api, query keys, model, component, page, test
│   ├── files/
│   ├── chat/
│   ├── search/
│   └── auth/
└── shared/
    ├── api/                # Axios instance/interceptor
    ├── config/             # environment boundary
    ├── lib/                # utility thuần
    └── ui/                 # component không chứa business rule
```

Đây là feature-based architecture: code thay đổi cùng một nghiệp vụ nằm gần nhau. `app` chỉ compose ứng dụng; `shared` không import ngược từ `features`. Cách này tránh thư mục `components/services/hooks` toàn cục phình lớn khi nhiều team cùng phát triển.

## 3. Dependency và lý do

- React/React DOM: rendering và component model.
- TypeScript: contract rõ giữa API, domain model và UI.
- Vite: dev server/HMR/build nhanh; proxy `/api` sang port 8081.
- Tailwind CSS + Flowbite: token và utility responsive; Flowbite giúp đồng bộ hướng thiết kế Tailwind/Figma.
- React Router: route tree và layout route dùng chung AppShell.
- TanStack Query: cache, loading/error, retry, stale/refetch cho server state.
- Axios: HTTP client tập trung base URL, timeout, header và interceptor.
- Zustand: chỉ lưu trạng thái UI ngắn hạn như sidebar và task selection.
- React Hook Form + Zod: form ít re-render, schema là nguồn validation duy nhất.
- Lucide: icon SVG có API thống nhất.
- `clsx` + `tailwind-merge`: compose class có điều kiện và xử lý xung đột Tailwind.
- Vitest + Testing Library + jsdom: test hành vi người dùng trong môi trường gần DOM.

## 4. Trả lời câu hỏi kiến thức

### Figma Auto Layout chuyển sang Flex/Grid như thế nào?

Auto Layout một chiều thường ánh xạ sang `display: flex`; direction thành `flex-row`/`flex-col`, gap thành `gap-*`, padding thành `p-*`, alignment thành `items-*` và distribution thành `justify-*`. Layout hai chiều như dashboard cards phù hợp CSS Grid. Không dịch từng frame thành absolute positioning vì sẽ mất responsive behavior.

### Design token là gì?

Token là tên ngữ nghĩa cho quyết định thiết kế: `brand-600`, `radius-card`, `shadow-card`, thay vì rải literal color/radius khắp code. Khi brand thay đổi, sửa token thay vì sửa từng component. PER-8 khai báo token trong Tailwind `@theme`.

### Tailwind responsive hoạt động thế nào?

Thiết kế mobile-first: class không prefix áp dụng cho mobile; `sm:`, `lg:` mở rộng dần. Sidebar mobile dùng overlay/drawer, từ `lg` trở lên trở thành sidebar cố định. Dashboard lần lượt từ một cột sang ba cột.

### Server state và client state khác nhau?

Server state thuộc backend, có cache lifecycle, loading/error, stale/refetch và cần đồng bộ; TanStack Query quản lý. Client state là trạng thái giao diện cục bộ như sidebar đang mở; Zustand quản lý. Không copy response API vào Zustand vì tạo hai nguồn sự thật.

### Query key, staleTime và gcTime?

Query key là identity có cấu trúc của cache, ví dụ `['tasks', 'list', filters]`; filter đổi thì cache entry đổi. `staleTime` là thời gian dữ liệu được xem là còn mới. `gcTime` là thời gian cache không còn observer được giữ trước khi thu gom. Cấu hình hiện tại: stale 30 giây, GC 5 phút.

### Accessibility cần bảo đảm gì?

- Input có label thật; icon-only button có accessible name.
- Dùng đúng element `button`, `nav`, `main`, `aside`, heading hierarchy.
- Có focus-visible rõ, keyboard activation tự nhiên và `aria-live` cho toast.
- Mobile overlay có nút đóng; Modal dùng `role=dialog` và `aria-modal`.

Trong story tiếp theo nên bổ sung focus trap/restore focus cho Modal/Drawer bằng primitive được kiểm thử kỹ hoặc thư viện headless phù hợp.

## 5. Quyết định kỹ thuật và trade-off

- React Router v7 được dùng theo data-router API nhưng chưa dùng framework mode vì đây là SPA kết nối Spring Boot.
- Preview Task data giúp shell dùng được khi backend chưa bật. `VITE_ENABLE_API=true` chuyển query sang API; đây là bước nối tạm rõ ràng, không giả lập API ngầm.
- Flowbite được cài và scan bởi Tailwind; primitives lõi tự bọc để giữ API thiết kế của project và tránh feature phụ thuộc trực tiếp một vendor.
- Error Boundary bắt render error; API errors được xử lý trong query state. Hai loại lỗi có lifecycle khác nhau.
- `npm audit` tại thời điểm triển khai báo advisory React Router liên quan RSC/Server Actions.
  Ứng dụng này chạy Vite SPA, không dùng RSC, SSR, action endpoint hoặc server action nên
  đường thực thi bị ảnh hưởng không được bật. Project giữ bản mới nhất để nhận các bản vá
  còn lại; dependency này cần tiếp tục được theo dõi và nâng ngay khi upstream phát hành
  bản vá tương thích.

## 6. Definition of Done

- AppShell gồm Sidebar, Topbar, PageHeader/dashboard header.
- Button, Input, Select, Badge, Modal, Drawer, Skeleton, EmptyState, ErrorState.
- Desktop/mobile responsive.
- Router, QueryClient, Toast và ErrorBoundary providers.
- Proxy backend 8081 và `.env.example`.
- README có UI reference và screen list.
- Lint, behavior tests và production build phải pass trước khi đóng ticket.
