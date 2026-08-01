import { QueryClient } from '@tanstack/react-query'
import { createStatusMutationOptions, taskKeys, upsertTaskAcrossListCaches } from './taskQueries'
import type { PageResponse, Task } from '../model/task'

vi.mock('./taskApi', () => ({
  changeTaskStatus: vi.fn().mockRejectedValue(new Error('network unavailable')),
}))

const task: Task = {
  id: 'task-1',
  ownerId: 'owner-1',
  title: 'Rollback task',
  status: 'TODO',
  priority: 'MEDIUM',
  position: 0,
  version: 0,
  tagIds: [],
  createdAt: '2026-08-01T00:00:00Z',
  updatedAt: '2026-08-01T00:00:00Z',
}

describe('optimistic status mutation', () => {
  it('restores cached task state when the API rejects the change', async () => {
    const queryClient = new QueryClient()
    const filters = { page: 0, size: 8, sort: 'createdAt,desc' }
    const key = taskKeys.list(filters)
    const page: PageResponse<Task> = {
      content: [task],
      page: { size: 8, number: 0, totalElements: 1, totalPages: 1 },
    }
    queryClient.setQueryData(key, page)
    const options = createStatusMutationOptions(queryClient)
    const variables = { taskId: task.id, status: 'DONE' as const }

    const context = await options.onMutate(variables)
    expect(queryClient.getQueryData<PageResponse<Task>>(key)?.content[0].status).toBe('DONE')

    options.onError(new Error('network unavailable'), variables, context)

    expect(queryClient.getQueryData<PageResponse<Task>>(key)?.content[0].status).toBe('TODO')
  })
})

describe('targeted create cache update', () => {
  it('updates only matching list and metric caches without invalidating queries', () => {
    const queryClient = new QueryClient()
    const allFilters = { page: 0, size: 8, sort: 'createdAt,desc' }
    const todoFilters = { ...allFilters, size: 1, status: 'TODO' as const }
    const doneFilters = { ...allFilters, size: 1, status: 'DONE' as const }
    const emptyPage = (size: number): PageResponse<Task> => ({
      content: [],
      page: { size, number: 0, totalElements: 0, totalPages: 0 },
    })
    queryClient.setQueryData(taskKeys.list(allFilters), emptyPage(8))
    queryClient.setQueryData(taskKeys.list(todoFilters), emptyPage(1))
    queryClient.setQueryData(taskKeys.list(doneFilters), emptyPage(1))
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')

    upsertTaskAcrossListCaches(queryClient, task)

    expect(queryClient.getQueryData<PageResponse<Task>>(taskKeys.list(allFilters))?.content).toEqual([task])
    expect(queryClient.getQueryData<PageResponse<Task>>(taskKeys.list(todoFilters))?.page.totalElements).toBe(1)
    expect(queryClient.getQueryData<PageResponse<Task>>(taskKeys.list(doneFilters))?.page.totalElements).toBe(0)
    expect(invalidateSpy).not.toHaveBeenCalled()
  })
})
