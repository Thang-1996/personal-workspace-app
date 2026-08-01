import { CalendarDays, Circle, CircleCheckBig } from 'lucide-react'
import { memo } from 'react'
import type { Task, TaskList, TaskStatus } from '../model/task'
import { Badge } from '../../../shared/ui/Badge'
import { cn } from '../../../shared/lib/cn'

const priorityTone = { LOW: 'slate', MEDIUM: 'indigo', HIGH: 'amber', URGENT: 'amber' } as const

export const TaskCard = memo(function TaskCard({
  task,
  taskList,
  onOpen,
  onStatusChange,
}: {
  task: Task
  taskList?: TaskList
  onOpen: (taskId: string) => void
  onStatusChange: (taskId: string, status: TaskStatus) => void
}) {
  const complete = task.status === 'DONE'
  return (
    <article className="flex items-start gap-3 rounded-card border border-slate-200 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-card">
      <button
        aria-label={complete ? `Mark ${task.title} incomplete` : `Mark ${task.title} complete`}
        className="mt-0.5 text-brand-600"
        onClick={() => onStatusChange(task.id, complete ? 'TODO' : 'DONE')}
        type="button"
      >
        {complete ? <CircleCheckBig size={21} /> : <Circle size={21} />}
      </button>
      <button className="min-w-0 flex-1 text-left" onClick={() => onOpen(task.id)} type="button">
        <span className={cn('block font-semibold text-slate-900', complete && 'text-slate-400 line-through')}>{task.title}</span>
        <span className="mt-2 flex flex-wrap items-center gap-2">
          <Badge tone={priorityTone[task.priority]}>{task.priority}</Badge>
          <span className="text-xs text-slate-500">{taskList?.name ?? 'No list'}</span>
          {task.dueAt && <span className="inline-flex items-center gap-1 text-xs text-slate-500"><CalendarDays aria-hidden size={13} />{new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(new Date(task.dueAt))}</span>}
        </span>
      </button>
    </article>
  )
})
