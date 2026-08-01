import { httpClient } from '../../../shared/api/httpClient'
import type { Task } from '../model/task'

type PageResponse<T> = { content: T[] }

export async function getTasks(): Promise<Task[]> {
  const response = await httpClient.get<PageResponse<Task>>('/v1/tasks', {
    params: { page: 0, size: 20, sort: 'createdAt,desc' },
  })
  return response.data.content
}
