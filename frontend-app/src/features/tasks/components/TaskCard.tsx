import { CalendarDays, Circle, CircleCheckBig } from 'lucide-react'
import type { Task } from '../model/task'
import { Badge } from '../../../shared/ui/Badge'
import { cn } from '../../../shared/lib/cn'

const priorityTone = {
  LOW: 'slate',
  MEDIUM: 'indigo',
  HIGH: 'amber',
  URGENT: 'amber',
} as const

export function TaskCard({ task }: { task: Task }) {
  const complete = task.status === 'DONE'

  return (
    <article className="flex items-start gap-3 rounded-card border border-slate-200 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-card">
      <button aria-label={complete ? `Mark ${task.title} incomplete` : `Mark ${task.title} complete`} className="mt-0.5 text-brand-600" type="button">
        {complete ? <CircleCheckBig size={21} /> : <Circle size={21} />}
      </button>
      <div className="min-w-0 flex-1">
        <p className={cn('font-semibold text-slate-900', complete && 'text-slate-400 line-through')}>{task.title}</p>
        <div className="mt-2 flex flex-wrap items-center gap-2">
          <Badge tone={priorityTone[task.priority]}>{task.priority}</Badge>
          <span className="text-xs text-slate-500">{task.listName}</span>
          <span className="inline-flex items-center gap-1 text-xs text-slate-500"><CalendarDays aria-hidden size={13} />{task.dueLabel}</span>
        </div>
      </div>
    </article>
  )
}
