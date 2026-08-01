import type { PropsWithChildren } from 'react'
import { Button } from '../../shared/ui/Button'
import { Skeleton } from '../../shared/ui/States'
import { useAuth } from './authContext'

export function ProtectedRoute({ children }: PropsWithChildren) {
  const { authenticated, initialized, login } = useAuth()

  if (!initialized) return <div className="mx-auto max-w-md p-8"><Skeleton /></div>
  if (authenticated) return children

  return (
    <main className="grid min-h-screen place-items-center bg-slate-50 p-6">
      <section className="w-full max-w-md rounded-card bg-white p-8 text-center shadow-card">
        <p className="text-sm font-semibold text-brand-600">Personal Workspace</p>
        <h1 className="mt-2 text-2xl font-bold text-slate-950">Sign in to your workspace</h1>
        <p className="mt-3 text-sm leading-6 text-slate-600">
          Authentication is handled by Keycloak using Authorization Code Flow with PKCE.
        </p>
        <Button className="mt-6 w-full" onClick={() => void login()}>
          Continue to sign in
        </Button>
      </section>
    </main>
  )
}
