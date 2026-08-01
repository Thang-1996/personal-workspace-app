# Personal Workspace UI

Enterprise-oriented React foundation for PER-8.

## Commands

```bash
npm install
npm run dev
npm run check
```

Vite serves the UI on `http://localhost:5173` and proxies `/api` to the Task Service on `http://localhost:8081`.
Copy `.env.example` to `.env.local` when the API base path needs to be overridden.

## Architecture

- `src/app`: composition root, providers, router, app shell and global UI state.
- `src/features`: business capabilities. Each feature owns its API, model, components, pages and tests.
- `src/shared/api`: configured infrastructure clients.
- `src/shared/config`: typed runtime configuration boundary.
- `src/shared/ui`: reusable, business-agnostic UI primitives.

Server state belongs to TanStack Query. Zustand is limited to ephemeral client UI state. Forms use React Hook Form and Zod.

## UI reference and screen list

The visual foundation follows the PER-8 wireframe direction: fixed navigation, compact top bar, page header, dashboard metrics and task cards. Tokens use indigo/blue primary colors, neutral slate, rounded cards and subtle shadows.

- Tasks dashboard: integrated with Task Service metrics.
- Task list: search, filters, URL state and pagination.
- Task creation modal: integrated with API and validation.
- Task detail drawer: view, edit, delete and optimistic status toggle.
- Files: routed placeholder for its feature story.
- Chat: routed placeholder for its feature story.
- Search: routed placeholder for its feature story.
- Profile: routed placeholder for its feature story.

The shell switches from a fixed desktop sidebar to an accessible mobile drawer.
