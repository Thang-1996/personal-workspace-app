import { zodResolver } from '@hookform/resolvers/zod'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { Button } from '../../../shared/ui/Button'
import { Input, Select } from '../../../shared/ui/FormControls'

const taskSchema = z.object({
  title: z.string().trim().min(3, 'Title must contain at least 3 characters').max(160),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']),
})

export type TaskFormValues = z.infer<typeof taskSchema>

export function CreateTaskForm({ onCancel, onSubmit }: {
  onCancel: () => void
  onSubmit: (values: TaskFormValues) => void
}) {
  const {
    control,
    handleSubmit,
    register,
    formState: { errors, isSubmitting },
  } = useForm<TaskFormValues>({
    resolver: zodResolver(taskSchema),
    defaultValues: { title: '', priority: 'MEDIUM' },
  })

  return (
    <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
      <label className="block">
        <span className="mb-1.5 block text-sm font-semibold text-slate-700">Task title</span>
        <Input aria-invalid={Boolean(errors.title)} autoFocus placeholder="What needs to be done?" {...register('title')} />
        {errors.title && <span className="mt-1 block text-xs text-red-600">{errors.title.message}</span>}
      </label>
      <label className="block">
        <span className="mb-1.5 block text-sm font-semibold text-slate-700">Priority</span>
        <Controller
          control={control}
          name="priority"
          render={({ field }) => (
            <Select className="w-full" {...field}>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="URGENT">Urgent</option>
            </Select>
          )}
        />
      </label>
      <div className="flex justify-end gap-2 pt-2">
        <Button onClick={onCancel} variant="secondary">Cancel</Button>
        <Button disabled={isSubmitting} type="submit">Create task</Button>
      </div>
    </form>
  )
}
