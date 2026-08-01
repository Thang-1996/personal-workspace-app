import axios from 'axios'
import { env } from '../config/env'

export const httpClient = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

httpClient.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error),
)
