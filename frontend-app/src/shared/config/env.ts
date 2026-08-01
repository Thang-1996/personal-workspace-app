const fallbackApiUrl = '/api'

export const env = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || fallbackApiUrl,
  enableApi: import.meta.env.VITE_ENABLE_API === 'true',
} as const
