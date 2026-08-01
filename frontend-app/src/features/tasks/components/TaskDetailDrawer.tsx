import { Link2, Trash2 } from 'lucide-react'
import { useCallback, useState } from 'react'
import type { TaskPayload } from '../model/task'
import { useDeleteTask, useTask, useTaskLists, useUpdateTask } from '../api/taskQueries'
import { useUiStore } from '../../../app/store/uiStore'
import { ApiError } from '../../../shared/api/httpClient'
import { Button } from '../../../shared/ui/Button'
import { Drawer } from '../../../shared/ui/Drawer'
import { ErrorState, Skeleton } from '../../../shared/ui/States'
import { useToast } from '../../../shared/ui/toastContext'
import { TaskForm } from './TaskForm'
import { useAttachFile, useFiles } from '../../files/api/fileQueries'
import { Select } from '../../../shared/ui/FormControls'

export function TaskDetailDrawer() {
  const selectedTaskId = useUiStore((state) => state.selectedTaskId)
  const selectTask = useUiStore((state) => state.selectTask)
  const taskQuery = useTask(selectedTaskId)
  const listsQuery = useTaskLists()
  const updateMutation = useUpdateTask()
  const deleteMutation = useDeleteTask()
  const rootFiles = useFiles(undefined, Boolean(selectedTaskId))
  const attachFile = useAttachFile()
  const [attachmentId, setAttachmentId] = useState('')
  const { notify } = useToast()
  const close = useCallback(() => selectTask(null), [selectTask])

  async function update(payload: TaskPayload) {
    if (!taskQuery.data) return
    try {
      await updateMutation.mutateAsync({
        taskId: taskQuery.data.id,
        payload: { ...payload, status: taskQuery.data.status },
      })
      notify('Task updated.')
      close()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Task could not be updated.')
    }
  }

  async function remove() {
    if (!selectedTaskId || !window.confirm('Delete this task permanently?')) return
    try {
      await deleteMutation.mutateAsync(selectedTaskId)
      notify('Task deleted.')
      close()
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Task could not be deleted.')
    }
  }

  async function attach() {
    if (!selectedTaskId || !attachmentId) return
    try {
      await attachFile.mutateAsync({ fileId: attachmentId, taskId: selectedTaskId })
      setAttachmentId('')
      notify('File attached to task.')
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'File could not be attached.')
    }
  }

  return (
    <Drawer onClose={close} open={Boolean(selectedTaskId)} title="Task details">
      {taskQuery.isLoading && <Skeleton />}
      {taskQuery.isError && <ErrorState message={taskQuery.error instanceof Error ? taskQuery.error.message : 'Task could not be loaded.'} />}
      {taskQuery.data && (
        <>
          <TaskForm
            onCancel={close}
            onSubmit={update}
            task={taskQuery.data}
            taskLists={listsQuery.data ?? []}
          />
          <div className="mt-6 border-t border-slate-200 pt-5">
            <h3 className="text-sm font-bold text-slate-900">Attachment</h3>
            <div className="mt-3 flex gap-2">
              <Select
                aria-label="Choose a file to attach"
                className="min-w-0 flex-1"
                onChange={(event) => setAttachmentId(event.target.value)}
                value={attachmentId}
              >
                <option value="">Choose a root file</option>
                {rootFiles.data?.map((file) => (
                  <option key={file.id} value={file.id}>{file.originalName}</option>
                ))}
              </Select>
              <Button disabled={!attachmentId || attachFile.isPending} onClick={() => void attach()} variant="secondary">
                <Link2 size={17} />Attach
              </Button>
            </div>
            {rootFiles.isError && <p className="mt-2 text-xs text-red-600">Files could not be loaded.</p>}
          </div>
          <div className="mt-6 border-t border-slate-200 pt-5">
            <Button className="text-red-600 hover:bg-red-50" disabled={deleteMutation.isPending} onClick={remove} variant="ghost">
              <Trash2 size={17} />Delete task
            </Button>
          </div>
        </>
      )}
    </Drawer>
  )
}
