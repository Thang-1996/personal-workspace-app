import { useEffect, type PropsWithChildren } from 'react'
import { Skeleton } from '../../shared/ui/States'
import { useAuth } from './authContext'

export function ProtectedRoute({ children }: PropsWithChildren) {
  const { authenticated, initialized, login } = useAuth()

  useEffect(() => {
    if (initialized && !authenticated) void login()
  }, [authenticated, initialized, login])

  if (!initialized) return <div className="mx-auto max-w-md p-8"><Skeleton /></div>
  if (authenticated) return children

  return <div className="mx-auto max-w-md p-8"><Skeleton /></div>
}
