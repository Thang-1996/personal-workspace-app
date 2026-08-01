import axios, { AxiosError } from 'axios'
import { env } from '../config/env'

export type ProblemDetails = {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  properties?: Record<string, unknown>
}

export class ApiError extends Error {
  readonly status: number
  readonly problem?: ProblemDetails

  constructor(message: string, status = 0, problem?: ProblemDetails) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

export const httpClient = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

let accessToken: string | undefined

export function setAccessToken(token?: string) {
  accessToken = token
}

httpClient.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

httpClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ProblemDetails>) => {
    const problem = error.response?.data
    const message = problem?.detail || problem?.title || error.message || 'The request could not be completed.'
    return Promise.reject(new ApiError(message, error.response?.status, problem))
  },
)
