import Keycloak, { type KeycloakTokenParsed } from 'keycloak-js'
import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type PropsWithChildren } from 'react'
import { setAccessToken } from '../../shared/api/httpClient'
import { env } from '../../shared/config/env'
import { AuthContext, type WorkspaceUser } from './authContext'
import { currentApplicationUrl } from './redirectUri'

type WorkspaceToken = KeycloakTokenParsed & {
  preferred_username?: string
  name?: string
  realm_access?: { roles?: string[] }
}

const keycloak = new Keycloak(env.keycloak)
let initialization: Promise<boolean> | undefined

function initializeKeycloak() {
  initialization ??= keycloak.init({
    onLoad: 'login-required',
    pkceMethod: 'S256',
    checkLoginIframe: false,
    redirectUri: currentApplicationUrl(),
  })
  return initialization
}

export function AuthProvider({ children }: PropsWithChildren) {
  const queryClient = useQueryClient()
  const [initialized, setInitialized] = useState(false)
  const [initializationError, setInitializationError] = useState<string | null>(null)
  const [authenticated, setAuthenticated] = useState(false)
  const [user, setUser] = useState<WorkspaceUser | null>(null)

  const synchronizeSession = useCallback(() => {
    const token = keycloak.tokenParsed as WorkspaceToken | undefined
    setAccessToken(keycloak.token)
    setAuthenticated(Boolean(keycloak.authenticated))
    setUser(
      keycloak.authenticated
        ? {
            displayName: token?.name || token?.preferred_username || 'Workspace user',
            username: token?.preferred_username,
            roles: token?.realm_access?.roles ?? [],
          }
        : null,
    )
  }, [])

  useEffect(() => {
    let active = true
    initializeKeycloak()
      .then(() => {
        if (!active) return
        synchronizeSession()
        setInitialized(true)
      })
      .catch((error: unknown) => {
        if (!active) return
        setInitializationError(error instanceof Error ? error.message : 'Authentication initialization failed')
        setInitialized(true)
      })

    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).then(synchronizeSession).catch(() => void keycloak.login())
    }
    keycloak.onAuthLogout = () => {
      setAccessToken()
      queryClient.clear()
      synchronizeSession()
    }

    const refreshTimer = window.setInterval(() => {
      if (keycloak.authenticated) {
        keycloak.updateToken(60).then(synchronizeSession).catch(() => void keycloak.login())
      }
    }, 30_000)

    return () => {
      active = false
      window.clearInterval(refreshTimer)
    }
  }, [queryClient, synchronizeSession])

  const login = useCallback(async () => {
    await keycloak.login({ redirectUri: currentApplicationUrl() })
  }, [])

  const logout = useCallback(async () => {
    setAccessToken()
    queryClient.clear()
    await keycloak.logout({ redirectUri: window.location.origin })
  }, [queryClient])

  const value = useMemo(
    () => ({
      authenticated,
      initialized,
      initializationError,
      user,
      hasRole: (role: string) => user?.roles.includes(role) ?? false,
      login,
      logout,
    }),
    [authenticated, initializationError, initialized, login, logout, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
