import { useQuery } from '@tanstack/react-query'
import { env } from '../../../shared/config/env'
import type { Task } from '../model/task'
import { getTasks } from './taskApi'

export const taskKeys = {
  all: ['tasks'] as const,
  list: (filters: Record<string, unknown>) => [...taskKeys.all, 'list', filters] as const,
}

const previewTasks: Task[] = [
  { id: '1', title: 'Finalize day-one architecture', status: 'IN_PROGRESS', priority: 'HIGH', dueLabel: 'Today', listName: 'Work' },
  { id: '2', title: 'Review product discovery notes', status: 'TODO', priority: 'MEDIUM', dueLabel: 'Tomorrow', listName: 'Planning' },
  { id: '3', title: 'Prepare weekly learning plan', status: 'TODO', priority: 'LOW', dueLabel: 'Fri', listName: 'Personal' },
  { id: '4', title: 'Publish project documentation', status: 'DONE', priority: 'MEDIUM', dueLabel: 'Completed', listName: 'Work' },
]

export function useTasks() {
  return useQuery({
    queryKey: taskKeys.list({}),
    queryFn: getTasks,
    enabled: env.enableApi,
    initialData: previewTasks,
    initialDataUpdatedAt: Date.now(),
  })
}
