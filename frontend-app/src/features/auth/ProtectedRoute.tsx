import type { PropsWithChildren } from 'react'
import { Button } from '../../shared/ui/Button'
import { ErrorState, Skeleton } from '../../shared/ui/States'
import { useAuth } from './authContext'

export function ProtectedRoute({ children }: PropsWithChildren) {
  const { authenticated, initializationError, initialized, login } = useAuth()

  if (!initialized) return <div className="mx-auto max-w-md p-8"><Skeleton /></div>
  if (authenticated) return children

  return (
    <div className="mx-auto grid min-h-screen max-w-md place-content-center gap-4 p-8">
      <ErrorState message={initializationError || 'Your authentication session could not be established.'} />
      <Button onClick={() => void login()}>Try signing in again</Button>
    </div>
  )
}
