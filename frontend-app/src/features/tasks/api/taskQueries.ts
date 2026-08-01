import {
  useMutation,
  useQueries,
  useQuery,
  useQueryClient,
  type QueryClient,
} from '@tanstack/react-query'
import { useMemo } from 'react'
import type { PageResponse, Task, TaskFilters, TaskPayload, TaskStatus, UpdateTaskPayload } from '../model/task'
import {
  changeTaskStatus,
  createTask,
  deleteTask,
  getTask,
  getTaskLists,
  getTasks,
  updateTask,
} from './taskApi'

export const taskKeys = {
  all: ['tasks'] as const,
  lists: () => [...taskKeys.all, 'list'] as const,
  list: (filters: TaskFilters) => [...taskKeys.lists(), filters] as const,
  details: () => [...taskKeys.all, 'detail'] as const,
  detail: (taskId: string) => [...taskKeys.details(), taskId] as const,
}

export const taskListKeys = {
  all: ['task-lists'] as const,
  list: () => [...taskListKeys.all, 'list'] as const,
}

export function useTasks(filters: TaskFilters) {
  return useQuery({ queryKey: taskKeys.list(filters), queryFn: () => getTasks(filters) })
}

export function useTask(taskId: string | null) {
  return useQuery({
    queryKey: taskKeys.detail(taskId ?? ''),
    queryFn: () => getTask(taskId!),
    enabled: Boolean(taskId),
  })
}

export function useTaskLists() {
  return useQuery({ queryKey: taskListKeys.list(), queryFn: getTaskLists })
}

export function useCreateTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: TaskPayload) => createTask(payload),
    onSuccess: (task) => {
      upsertTaskAcrossListCaches(queryClient, task)
      queryClient.setQueryData(taskKeys.detail(task.id), task)
    },
  })
}

export function useUpdateTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ taskId, payload }: { taskId: string; payload: UpdateTaskPayload }) =>
      updateTask(taskId, payload),
    onSuccess: (task) => {
      const previousTask = findTaskInCache(queryClient, task.id)
      upsertTaskAcrossListCaches(queryClient, task, previousTask)
      queryClient.setQueryData(taskKeys.detail(task.id), task)
    },
  })
}

export function useDeleteTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteTask,
    onSuccess: (_data, taskId) => {
      const deletedTask = findTaskInCache(queryClient, taskId)
      if (deletedTask) removeTaskAcrossListCaches(queryClient, deletedTask)
      queryClient.removeQueries({ queryKey: taskKeys.detail(taskId) })
    },
  })
}

type OptimisticContext = {
  listSnapshots: Array<[readonly unknown[], PageResponse<Task> | undefined]>
  detailSnapshot: Task | undefined
}

export function createStatusMutationOptions(queryClient: QueryClient) {
  return {
    mutationFn: ({ taskId, status }: { taskId: string; status: TaskStatus }) =>
      changeTaskStatus(taskId, status),
    onMutate: async ({ taskId, status }: { taskId: string; status: TaskStatus }): Promise<OptimisticContext> => {
      await queryClient.cancelQueries({ queryKey: taskKeys.all })
      const listSnapshots = queryClient.getQueriesData<PageResponse<Task>>({ queryKey: taskKeys.lists() })
      const detailSnapshot = findTaskInCache(queryClient, taskId)
      const completedAt = status === 'DONE' ? new Date().toISOString() : null
      if (detailSnapshot) {
        const optimisticTask = {
          ...detailSnapshot,
          status,
          completedAt,
        }
        upsertTaskAcrossListCaches(queryClient, optimisticTask, detailSnapshot)
        queryClient.setQueryData<Task>(taskKeys.detail(taskId), optimisticTask)
      }
      return { listSnapshots, detailSnapshot }
    },
    onError: (_error: Error, variables: { taskId: string; status: TaskStatus }, context?: OptimisticContext) => {
      for (const [key, page] of context?.listSnapshots ?? []) queryClient.setQueryData(key, page)
      if (context?.detailSnapshot) {
        queryClient.setQueryData(taskKeys.detail(variables.taskId), context.detailSnapshot)
      }
    },
    onSuccess: (task: Task) => {
      const optimisticTask = findTaskInCache(queryClient, task.id)
      upsertTaskAcrossListCaches(queryClient, task, optimisticTask)
      queryClient.setQueryData(taskKeys.detail(task.id), task)
    },
  }
}

export function useChangeStatus() {
  const queryClient = useQueryClient()
  return useMutation(createStatusMutationOptions(queryClient))
}

function filtersFromKey(key: readonly unknown[]): TaskFilters | undefined {
  if (key[0] !== 'tasks' || key[1] !== 'list') return undefined
  return key[2] as TaskFilters | undefined
}

export function taskMatchesFilters(task: Task, filters: TaskFilters) {
  if (filters.status && task.status !== filters.status) return false
  if (filters.priority && task.priority !== filters.priority) return false
  if (filters.listId && task.taskListId !== filters.listId) return false
  if (filters.keyword) {
    const keyword = filters.keyword.trim().toLocaleLowerCase()
    const searchable = `${task.title} ${task.description ?? ''}`.toLocaleLowerCase()
    if (!searchable.includes(keyword)) return false
  }
  if (filters.dueFrom && (!task.dueAt || new Date(task.dueAt) < new Date(filters.dueFrom))) return false
  if (filters.dueTo && (!task.dueAt || new Date(task.dueAt) > new Date(filters.dueTo))) return false
  return true
}

function sortTasks(tasks: Task[], sort: string) {
  const [field, direction = 'asc'] = sort.split(',')
  const multiplier = direction === 'desc' ? -1 : 1
  return [...tasks].sort((left, right) => {
    const leftValue = field === 'priority' ? priorityRank(left.priority) : String(left[field as keyof Task] ?? '')
    const rightValue = field === 'priority' ? priorityRank(right.priority) : String(right[field as keyof Task] ?? '')
    return leftValue < rightValue ? -1 * multiplier : leftValue > rightValue ? multiplier : 0
  })
}

function priorityRank(priority: Task['priority']) {
  return { LOW: 1, MEDIUM: 2, HIGH: 3, URGENT: 4 }[priority]
}

function updatedPage(
  page: PageResponse<Task>,
  filters: TaskFilters,
  task: Task,
  previousTask?: Task,
) {
  const cachedTask = page.content.find((item) => item.id === task.id)
  const previous = previousTask ?? cachedTask
  const matchedBefore = previous ? taskMatchesFilters(previous, filters) : false
  const matchesNow = taskMatchesFilters(task, filters)
  const totalElements = Math.max(0, page.page.totalElements + Number(matchesNow) - Number(matchedBefore))
  let content = page.content.filter((item) => item.id !== task.id)

  const belongedToThisPage = Boolean(cachedTask)
  if (matchesNow && (belongedToThisPage || filters.page === 0)) {
    content = sortTasks([...content, task], filters.sort).slice(0, filters.size)
  }

  return {
    content,
    page: {
      ...page.page,
      totalElements,
      totalPages: totalElements === 0 ? 0 : Math.ceil(totalElements / page.page.size),
    },
  }
}

export function upsertTaskAcrossListCaches(
  queryClient: QueryClient,
  task: Task,
  previousTask?: Task,
) {
  for (const [key, page] of queryClient.getQueriesData<PageResponse<Task>>({ queryKey: taskKeys.lists() })) {
    const filters = filtersFromKey(key)
    if (!page || !filters) continue
    queryClient.setQueryData(key, updatedPage(page, filters, task, previousTask))
  }
}

export function removeTaskAcrossListCaches(queryClient: QueryClient, task: Task) {
  for (const [key, page] of queryClient.getQueriesData<PageResponse<Task>>({ queryKey: taskKeys.lists() })) {
    const filters = filtersFromKey(key)
    if (!page || !filters || !taskMatchesFilters(task, filters)) continue
    const totalElements = Math.max(0, page.page.totalElements - 1)
    queryClient.setQueryData<PageResponse<Task>>(key, {
      content: page.content.filter((item) => item.id !== task.id),
      page: {
        ...page.page,
        totalElements,
        totalPages: totalElements === 0 ? 0 : Math.ceil(totalElements / page.page.size),
      },
    })
  }
}

function findTaskInCache(queryClient: QueryClient, taskId: string) {
  const detail = queryClient.getQueryData<Task>(taskKeys.detail(taskId))
  if (detail) return detail
  for (const [, page] of queryClient.getQueriesData<PageResponse<Task>>({ queryKey: taskKeys.lists() })) {
    const task = page?.content.find((item) => item.id === taskId)
    if (task) return task
  }
  return undefined
}

function dayBounds(offsetDays = 0) {
  const start = new Date()
  start.setDate(start.getDate() + offsetDays)
  start.setHours(0, 0, 0, 0)
  const end = new Date(start)
  end.setHours(23, 59, 59, 999)
  return { dueFrom: start.toISOString(), dueTo: end.toISOString() }
}

const statsBase = { page: 0, size: 1, sort: 'createdAt,desc' } as const

export function useDashboardStats() {
  const today = useMemo(() => dayBounds(), [])
  const overdueTo = useMemo(() => new Date(Date.now() - 1).toISOString(), [])
  const activeStatuses: TaskStatus[] = ['TODO', 'IN_PROGRESS']
  const queries = useQueries({
    queries: [
      ...activeStatuses.map((status) => ({
        queryKey: taskKeys.list({ ...statsBase, status }),
        queryFn: () => getTasks({ ...statsBase, status }),
      })),
      ...activeStatuses.map((status) => ({
        queryKey: taskKeys.list({ ...statsBase, status, ...today }),
        queryFn: () => getTasks({ ...statsBase, status, ...today }),
      })),
      ...activeStatuses.map((status) => ({
        queryKey: taskKeys.list({ ...statsBase, status, dueTo: overdueTo }),
        queryFn: () => getTasks({ ...statsBase, status, dueTo: overdueTo }),
      })),
      {
        queryKey: taskKeys.list({ ...statsBase, status: 'DONE' }),
        queryFn: () => getTasks({ ...statsBase, status: 'DONE' }),
      },
    ],
  })
  const totals = queries.map((query) => query.data?.page.totalElements ?? 0)
  return {
    open: totals[0] + totals[1],
    today: totals[2] + totals[3],
    overdue: totals[4] + totals[5],
    completed: totals[6],
    isLoading: queries.some((query) => query.isLoading),
  }
}
