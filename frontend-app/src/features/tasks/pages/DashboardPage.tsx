import { CheckCircle2, Clock3, ListTodo, Plus } from 'lucide-react'
import { useState } from 'react'
import { useTasks } from '../api/taskQueries'
import { CreateTaskForm, type TaskFormValues } from '../components/CreateTaskForm'
import { TaskCard } from '../components/TaskCard'
import { Button } from '../../../shared/ui/Button'
import { Modal } from '../../../shared/ui/Modal'
import { ErrorState, Skeleton } from '../../../shared/ui/States'
import { useToast } from '../../../shared/ui/toastContext'

const metrics = [
  { label: 'Open tasks', value: '12', icon: ListTodo, tone: 'bg-brand-50 text-brand-700' },
  { label: 'Due today', value: '3', icon: Clock3, tone: 'bg-amber-50 text-amber-700' },
  { label: 'Completed', value: '24', icon: CheckCircle2, tone: 'bg-emerald-50 text-emerald-700' },
]

export function DashboardPage() {
  const [createOpen, setCreateOpen] = useState(false)
  const { data: tasks, isError, isLoading } = useTasks()
  const { notify } = useToast()

  function createTask(values: TaskFormValues) {
    notify(`Task “${values.title}” is ready to connect to the API.`)
    setCreateOpen(false)
  }

  return (
    <>
      <section aria-labelledby="dashboard-title">
        <header className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="mb-1 text-sm font-semibold text-brand-600">Saturday, August 1</p>
            <h1 className="text-3xl font-bold tracking-tight text-slate-950" id="dashboard-title">Good morning, Minh Thắng</h1>
            <p className="mt-2 text-slate-500">Stay focused on the work that moves your day forward.</p>
          </div>
          <Button onClick={() => setCreateOpen(true)}><Plus aria-hidden size={18} />New task</Button>
        </header>

        <div className="mb-8 grid gap-4 sm:grid-cols-3">
          {metrics.map(({ icon: Icon, label, tone, value }) => (
            <article className="rounded-card border border-slate-200 bg-white p-5 shadow-sm" key={label}>
              <div className={`mb-4 grid size-10 place-items-center rounded-xl ${tone}`}><Icon aria-hidden size={20} /></div>
              <p className="text-2xl font-bold text-slate-950">{value}</p>
              <p className="mt-1 text-sm text-slate-500">{label}</p>
            </article>
          ))}
        </div>

        <div className="mb-4 flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold text-slate-950">Today’s focus</h2>
            <p className="mt-1 text-sm text-slate-500">Your highest-impact tasks in one view.</p>
          </div>
          <Button variant="ghost">View all</Button>
        </div>

        <div className="grid gap-3 lg:grid-cols-2">
          {isLoading && <Skeleton />}
          {isError && <ErrorState message="Tasks could not be loaded from the API." />}
          {tasks?.map((task) => <TaskCard key={task.id} task={task} />)}
        </div>
      </section>

      <Modal onClose={() => setCreateOpen(false)} open={createOpen} title="Create a task">
        <CreateTaskForm onCancel={() => setCreateOpen(false)} onSubmit={createTask} />
      </Modal>
    </>
  )
}
