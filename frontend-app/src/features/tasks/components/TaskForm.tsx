import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { Task, TaskList, TaskPayload, TaskPriority } from '../model/task'
import { Button } from '../../../shared/ui/Button'
import { Input, Select } from '../../../shared/ui/FormControls'

const taskSchema = z.object({
  title: z.string().trim().min(3, 'Title must contain at least 3 characters').max(200),
  description: z.string().max(2000).optional(),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']),
  taskListId: z.string().optional(),
  dueAt: z.string().optional(),
})

export type TaskFormValues = z.infer<typeof taskSchema>

function defaults(task?: Task): TaskFormValues {
  return {
    title: task?.title ?? '',
    description: task?.description ?? '',
    priority: task?.priority ?? 'MEDIUM',
    taskListId: task?.taskListId ?? '',
    dueAt: task?.dueAt ? task.dueAt.slice(0, 16) : '',
  }
}

export function TaskForm({
  task,
  taskLists,
  onCancel,
  onSubmit,
  submitLabel = task ? 'Save changes' : 'Create task',
}: {
  task?: Task
  taskLists: TaskList[]
  onCancel: () => void
  onSubmit: (values: TaskPayload) => Promise<void> | void
  submitLabel?: string
}) {
  const {
    handleSubmit,
    register,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<TaskFormValues>({ resolver: zodResolver(taskSchema), defaultValues: defaults(task) })

  useEffect(() => reset(defaults(task)), [reset, task])

  async function submit(values: TaskFormValues) {
    await onSubmit({
      ...values,
      priority: values.priority as TaskPriority,
      dueAt: values.dueAt ? new Date(values.dueAt).toISOString() : undefined,
    })
  }

  return (
    <form className="space-y-4" onSubmit={handleSubmit(submit)}>
      <label className="block">
        <span className="mb-1.5 block text-sm font-semibold text-slate-700">Task title</span>
        <Input aria-invalid={Boolean(errors.title)} autoFocus placeholder="What needs to be done?" {...register('title')} />
        {errors.title && <span className="mt-1 block text-xs text-red-600">{errors.title.message}</span>}
      </label>
      <label className="block">
        <span className="mb-1.5 block text-sm font-semibold text-slate-700">Description</span>
        <textarea className="min-h-24 w-full rounded-xl border border-slate-200 bg-white p-3 text-sm" placeholder="Add context or notes…" {...register('description')} />
      </label>
      <div className="grid gap-4 sm:grid-cols-2">
        <label className="block">
          <span className="mb-1.5 block text-sm font-semibold text-slate-700">Priority</span>
          <Select className="w-full" {...register('priority')}>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="URGENT">Urgent</option>
          </Select>
        </label>
        <label className="block">
          <span className="mb-1.5 block text-sm font-semibold text-slate-700">List</span>
          <Select className="w-full" {...register('taskListId')}>
            <option value="">No list</option>
            {taskLists.filter((list) => !list.archived).map((list) => <option key={list.id} value={list.id}>{list.name}</option>)}
          </Select>
        </label>
      </div>
      <label className="block">
        <span className="mb-1.5 block text-sm font-semibold text-slate-700">Due date</span>
        <Input type="datetime-local" {...register('dueAt')} />
      </label>
      <div className="flex justify-end gap-2 pt-2">
        <Button onClick={onCancel} variant="secondary">Cancel</Button>
        <Button disabled={isSubmitting} type="submit">{isSubmitting ? 'Saving…' : submitLabel}</Button>
      </div>
    </form>
  )
}
