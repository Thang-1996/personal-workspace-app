import { createBrowserRouter, Navigate } from 'react-router-dom'
import { AppShell } from './shell/AppShell'
import { PlaceholderPage } from '../shared/ui/PlaceholderPage'
import { TasksRoute } from './RoutedPages'

export const router = createBrowserRouter([
  {
    element: <AppShell />,
    children: [
      { index: true, element: <Navigate replace to="/tasks" /> },
      { path: '/tasks', element: <TasksRoute /> },
      { path: '/files', element: <PlaceholderPage title="Files" /> },
      { path: '/chat', element: <PlaceholderPage title="Chat" /> },
      { path: '/search', element: <PlaceholderPage title="Search" /> },
      { path: '/profile', element: <PlaceholderPage title="Profile" /> },
    ],
  },
])
