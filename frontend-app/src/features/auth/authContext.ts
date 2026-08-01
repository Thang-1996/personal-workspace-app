import { createContext, useContext } from 'react'

export type WorkspaceUser = {
  displayName: string
  username?: string
  roles: string[]
}

export type AuthContextValue = {
  authenticated: boolean
  initialized: boolean
  user: WorkspaceUser | null
  hasRole: (role: string) => boolean
  login: () => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used inside AuthProvider')
  return value
}
