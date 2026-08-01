export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'ARCHIVED'

export type Task = {
  id: string
  ownerId: string
  title: string
  description?: string | null
  status: TaskStatus
  priority: TaskPriority
  dueAt?: string | null
  completedAt?: string | null
  position: number
  version: number
  taskListId?: string | null
  tagIds: string[]
  createdAt: string
  updatedAt: string
}

export type TaskList = {
  id: string
  ownerId: string
  name: string
  description?: string | null
  color?: string | null
  position: number
  archived: boolean
  version: number
  createdAt: string
  updatedAt: string
}

export type TaskFilters = {
  status?: TaskStatus
  priority?: TaskPriority
  listId?: string
  keyword?: string
  dueFrom?: string
  dueTo?: string
  page: number
  size: number
  sort: string
}

export type TaskPayload = {
  title: string
  description?: string
  priority: TaskPriority
  taskListId?: string
  dueAt?: string
  position?: number
  tagIds?: string[]
}

export type UpdateTaskPayload = TaskPayload & {
  status: TaskStatus
}

export type PageMetadata = {
  size: number
  number: number
  totalElements: number
  totalPages: number
}

export type PageResponse<T> = {
  content: T[]
  page: PageMetadata
}
