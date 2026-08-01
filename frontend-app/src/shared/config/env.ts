const fallbackApiUrl = '/api'

export const env = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || fallbackApiUrl,
  keycloak: {
    url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8090',
    realm: import.meta.env.VITE_KEYCLOAK_REALM || 'personal-workspace',
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'workspace-web',
  },
} as const
