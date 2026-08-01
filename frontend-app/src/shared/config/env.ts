const fallbackApiUrl = '/api'

export const env = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || fallbackApiUrl,
} as const
