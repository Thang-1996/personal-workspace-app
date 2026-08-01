import { httpClient } from '../../../shared/api/httpClient'
import type {
  PageResponse,
  Task,
  TaskFilters,
  TaskList,
  TaskPayload,
  TaskStatus,
  UpdateTaskPayload,
} from '../model/task'

export async function getTasks(filters: TaskFilters): Promise<PageResponse<Task>> {
  const response = await httpClient.get<PageResponse<Task>>('/v1/tasks', { params: filters })
  return response.data
}

export async function getTask(taskId: string): Promise<Task> {
  const response = await httpClient.get<Task>(`/v1/tasks/${taskId}`)
  return response.data
}

export async function getTaskLists(): Promise<TaskList[]> {
  const response = await httpClient.get<TaskList[]>('/v1/task-lists')
  return response.data
}

export async function createTask(payload: TaskPayload): Promise<Task> {
  const response = await httpClient.post<Task>('/v1/tasks', cleanPayload(payload))
  return response.data
}

export async function updateTask(taskId: string, payload: UpdateTaskPayload): Promise<Task> {
  const response = await httpClient.put<Task>(`/v1/tasks/${taskId}`, cleanPayload(payload))
  return response.data
}

export async function deleteTask(taskId: string): Promise<void> {
  await httpClient.delete(`/v1/tasks/${taskId}`)
}

export async function changeTaskStatus(taskId: string, status: TaskStatus): Promise<Task> {
  const response = await httpClient.patch<Task>(`/v1/tasks/${taskId}/status`, { status })
  return response.data
}

function cleanPayload<T extends TaskPayload>(payload: T): T {
  return Object.fromEntries(
    Object.entries(payload).filter(([, value]) => value !== '' && value !== undefined),
  ) as T
}
