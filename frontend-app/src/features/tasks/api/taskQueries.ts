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
      queryClient.setQueryData(taskKeys.detail(task.id), task)
      return queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
    },
  })
}

export function useUpdateTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ taskId, payload }: { taskId: string; payload: UpdateTaskPayload }) =>
      updateTask(taskId, payload),
    onSuccess: (task) => {
      queryClient.setQueryData(taskKeys.detail(task.id), task)
      return queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
    },
  })
}

export function useDeleteTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteTask,
    onSuccess: (_data, taskId) => {
      queryClient.removeQueries({ queryKey: taskKeys.detail(taskId) })
      return queryClient.invalidateQueries({ queryKey: taskKeys.lists() })
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
      const detailSnapshot = queryClient.getQueryData<Task>(taskKeys.detail(taskId))
      const completedAt = status === 'DONE' ? new Date().toISOString() : null

      for (const [key, page] of listSnapshots) {
        if (!page) continue
        queryClient.setQueryData<PageResponse<Task>>(key, {
          ...page,
          content: page.content.map((task) =>
            task.id === taskId ? { ...task, status, completedAt } : task,
          ),
        })
      }
      if (detailSnapshot) {
        queryClient.setQueryData<Task>(taskKeys.detail(taskId), {
          ...detailSnapshot,
          status,
          completedAt,
        })
      }
      return { listSnapshots, detailSnapshot }
    },
    onError: (_error: Error, variables: { taskId: string; status: TaskStatus }, context?: OptimisticContext) => {
      for (const [key, page] of context?.listSnapshots ?? []) queryClient.setQueryData(key, page)
      if (context?.detailSnapshot) {
        queryClient.setQueryData(taskKeys.detail(variables.taskId), context.detailSnapshot)
      }
    },
    onSuccess: (task: Task) => queryClient.setQueryData(taskKeys.detail(task.id), task),
    onSettled: () => queryClient.invalidateQueries({ queryKey: taskKeys.all }),
  }
}

export function useChangeStatus() {
  const queryClient = useQueryClient()
  return useMutation(createStatusMutationOptions(queryClient))
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
