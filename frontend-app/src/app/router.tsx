import { createBrowserRouter, Navigate } from 'react-router-dom'
import { AppShell } from './shell/AppShell'
import { PlaceholderPage } from '../shared/ui/PlaceholderPage'
import { FilesRoute, TasksRoute } from './RoutedPages'
import { ProtectedRoute } from '../features/auth/ProtectedRoute'

export const router = createBrowserRouter([
  {
    element: <ProtectedRoute><AppShell /></ProtectedRoute>,
    children: [
      { index: true, element: <Navigate replace to="/tasks" /> },
      { path: '/tasks', element: <TasksRoute /> },
      { path: '/files', element: <FilesRoute /> },
      { path: '/chat', element: <PlaceholderPage title="Chat" /> },
      { path: '/search', element: <PlaceholderPage title="Search" /> },
      { path: '/profile', element: <PlaceholderPage title="Profile" /> },
    ],
  },
])
