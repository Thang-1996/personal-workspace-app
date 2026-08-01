import { CheckCircle2, ChevronLeft, ChevronRight, Clock3, ListTodo, Plus, Search, TriangleAlert } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import type { TaskFilters, TaskPayload, TaskPriority, TaskStatus } from '../model/task'
import { useChangeStatus, useCreateTask, useDashboardStats, useTaskLists, useTasks } from '../api/taskQueries'
import { TaskCard } from '../components/TaskCard'
import { TaskDetailDrawer } from '../components/TaskDetailDrawer'
import { TaskForm } from '../components/TaskForm'
import { useUiStore } from '../../../app/store/uiStore'
import { ApiError } from '../../../shared/api/httpClient'
import { useDebouncedValue } from '../../../shared/lib/useDebouncedValue'
import { Button } from '../../../shared/ui/Button'
import { Input, Select } from '../../../shared/ui/FormControls'
import { Modal } from '../../../shared/ui/Modal'
import { EmptyState, ErrorState, Skeleton } from '../../../shared/ui/States'
import { useToast } from '../../../shared/ui/toastContext'

const pageSize = 8

function filtersFrom(searchParams: URLSearchParams): TaskFilters {
  return {
    status: (searchParams.get('status') as TaskStatus) || undefined,
    priority: (searchParams.get('priority') as TaskPriority) || undefined,
    listId: searchParams.get('listId') || undefined,
    keyword: searchParams.get('keyword') || undefined,
    page: Math.max(0, Number(searchParams.get('page') || 0)),
    size: pageSize,
    sort: searchParams.get('sort') || 'createdAt,desc',
  }
}

export function DashboardPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const filters = useMemo(() => filtersFrom(searchParams), [searchParams])
  const [keyword, setKeyword] = useState(filters.keyword ?? '')
  const [createOpen, setCreateOpen] = useState(false)
  const debouncedKeyword = useDebouncedValue(keyword)
  const tasksQuery = useTasks(filters)
  const listsQuery = useTaskLists()
  const stats = useDashboardStats()
  const createMutation = useCreateTask()
  const statusMutation = useChangeStatus()
  const selectTask = useUiStore((state) => state.selectTask)
  const { notify } = useToast()

  useEffect(() => {
    const current = searchParams.get('keyword') ?? ''
    if (current === debouncedKeyword) return
    setSearchParams((previous) => {
      const next = new URLSearchParams(previous)
      if (debouncedKeyword) next.set('keyword', debouncedKeyword)
      else next.delete('keyword')
      next.delete('page')
      return next
    }, { replace: true })
  }, [debouncedKeyword, searchParams, setSearchParams])

  const taskListsById = useMemo(
    () => new Map((listsQuery.data ?? []).map((list) => [list.id, list])),
    [listsQuery.data],
  )

  const updateFilter = useCallback((name: string, value: string) => {
    setSearchParams((previous) => {
      const next = new URLSearchParams(previous)
      if (value) next.set(name, value)
      else next.delete(name)
      if (name !== 'page') next.delete('page')
      return next
    })
  }, [setSearchParams])

  const changeStatus = useCallback(async (taskId: string, status: TaskStatus) => {
    try {
      await statusMutation.mutateAsync({ taskId, status })
      notify(status === 'DONE' ? 'Task completed.' : 'Task reopened.')
    } catch (error) {
      notify(error instanceof ApiError ? `${error.message} Previous status restored.` : 'Update failed. Previous status restored.')
    }
  }, [notify, statusMutation])

  async function create(payload: TaskPayload) {
    try {
      await createMutation.mutateAsync(payload)
      notify('Task created.')
      setCreateOpen(false)
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Task could not be created.')
      throw error
    }
  }

  const metrics = [
    { label: 'Open tasks', value: stats.open, icon: ListTodo, tone: 'bg-brand-50 text-brand-700' },
    { label: 'Due today', value: stats.today, icon: Clock3, tone: 'bg-amber-50 text-amber-700' },
    { label: 'Overdue', value: stats.overdue, icon: TriangleAlert, tone: 'bg-red-50 text-red-700' },
    { label: 'Completed', value: stats.completed, icon: CheckCircle2, tone: 'bg-emerald-50 text-emerald-700' },
  ]

  const page = tasksQuery.data?.page

  return (
    <>
      <section aria-labelledby="dashboard-title">
        <header className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="mb-1 text-sm font-semibold text-brand-600">Your personal command center</p>
            <h1 className="text-3xl font-bold tracking-tight text-slate-950" id="dashboard-title">Tasks dashboard</h1>
            <p className="mt-2 text-slate-500">Plan, search and complete work without leaving this view.</p>
          </div>
          <Button onClick={() => setCreateOpen(true)}><Plus aria-hidden size={18} />New task</Button>
        </header>

        <div className="mb-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {metrics.map(({ icon: Icon, label, tone, value }) => (
            <article className="rounded-card border border-slate-200 bg-white p-5 shadow-sm" key={label}>
              <div className={`mb-4 grid size-10 place-items-center rounded-xl ${tone}`}><Icon aria-hidden size={20} /></div>
              <p className="text-2xl font-bold text-slate-950">{stats.isLoading ? '—' : value}</p>
              <p className="mt-1 text-sm text-slate-500">{label}</p>
            </article>
          ))}
        </div>

        <section aria-labelledby="task-list-title" className="rounded-card border border-slate-200 bg-white p-4 shadow-sm sm:p-6">
          <div className="mb-5 flex flex-col gap-4">
            <div>
              <h2 className="text-xl font-bold text-slate-950" id="task-list-title">All tasks</h2>
              <p className="mt-1 text-sm text-slate-500">{page ? `${page.totalElements} tasks found` : 'Loading your tasks…'}</p>
            </div>
            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
              <label className="relative xl:col-span-2">
                <span className="sr-only">Search tasks</span>
                <Search className="pointer-events-none absolute left-3 top-2.5 text-slate-400" size={18} />
                <Input className="pl-10" onChange={(event) => setKeyword(event.target.value)} placeholder="Search title or description…" type="search" value={keyword} />
              </label>
              <Select aria-label="Filter by status" onChange={(event) => updateFilter('status', event.target.value)} value={filters.status ?? ''}>
                <option value="">All statuses</option>
                <option value="TODO">To do</option>
                <option value="IN_PROGRESS">In progress</option>
                <option value="DONE">Done</option>
                <option value="ARCHIVED">Archived</option>
              </Select>
              <Select aria-label="Filter by priority" onChange={(event) => updateFilter('priority', event.target.value)} value={filters.priority ?? ''}>
                <option value="">All priorities</option>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </Select>
              <Select aria-label="Filter by task list" onChange={(event) => updateFilter('listId', event.target.value)} value={filters.listId ?? ''}>
                <option value="">All lists</option>
                {(listsQuery.data ?? []).filter((list) => !list.archived).map((list) => <option key={list.id} value={list.id}>{list.name}</option>)}
              </Select>
            </div>
          </div>

          {tasksQuery.isLoading && <div className="grid gap-3 lg:grid-cols-2"><Skeleton /><Skeleton /><Skeleton /><Skeleton /></div>}
          {tasksQuery.isError && <ErrorState message={tasksQuery.error instanceof Error ? tasksQuery.error.message : 'Tasks could not be loaded.'} />}
          {tasksQuery.data?.content.length === 0 && <EmptyState message="No tasks match these filters. Create one or clear your filters." />}
          {tasksQuery.data && tasksQuery.data.content.length > 0 && (
            <div className="grid gap-3 lg:grid-cols-2">
              {tasksQuery.data.content.map((task) => (
                <TaskCard
                  key={task.id}
                  onOpen={selectTask}
                  onStatusChange={changeStatus}
                  task={task}
                  taskList={task.taskListId ? taskListsById.get(task.taskListId) : undefined}
                />
              ))}
            </div>
          )}

          {page && page.totalPages > 1 && (
            <nav aria-label="Task list pagination" className="mt-6 flex items-center justify-between border-t border-slate-200 pt-4">
              <p className="text-sm text-slate-500">Page {page.number + 1} of {page.totalPages}</p>
              <div className="flex gap-2">
                <Button aria-label="Previous page" disabled={page.number === 0} onClick={() => updateFilter('page', String(page.number - 1))} size="icon" variant="secondary"><ChevronLeft size={18} /></Button>
                <Button aria-label="Next page" disabled={page.number + 1 >= page.totalPages} onClick={() => updateFilter('page', String(page.number + 1))} size="icon" variant="secondary"><ChevronRight size={18} /></Button>
              </div>
            </nav>
          )}
        </section>
      </section>

      <Modal onClose={() => setCreateOpen(false)} open={createOpen} title="Create a task">
        <TaskForm onCancel={() => setCreateOpen(false)} onSubmit={create} taskLists={listsQuery.data ?? []} />
      </Modal>
      <TaskDetailDrawer />
    </>
  )
}
