import { lazy, Suspense, type ReactNode } from 'react'
import { Skeleton } from '../shared/ui/States'

const DashboardPage = lazy(() =>
  import('../features/tasks/pages/DashboardPage').then((module) => ({
    default: module.DashboardPage,
  })),
)

function AsyncPage({ children }: { children: ReactNode }) {
  return <Suspense fallback={<Skeleton />}>{children}</Suspense>
}

export function TasksRoute() {
  return <AsyncPage><DashboardPage /></AsyncPage>
}
