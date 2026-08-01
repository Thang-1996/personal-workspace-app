import {
  ChevronRight,
  Download,
  FileText,
  Folder,
  FolderPlus,
  Link2,
  Trash2,
} from 'lucide-react'
import { useState } from 'react'
import { Button } from '../../../shared/ui/Button'
import { Input } from '../../../shared/ui/FormControls'
import { Modal } from '../../../shared/ui/Modal'
import { EmptyState, ErrorState, Skeleton } from '../../../shared/ui/States'
import { useToast } from '../../../shared/ui/toastContext'
import { downloadFile } from '../api/fileApi'
import {
  useAttachFile,
  useCreateFolder,
  useDeleteFile,
  useFiles,
  useFolders,
  useUploadFile,
} from '../api/fileQueries'
import { FileDropzone } from '../components/FileDropzone'
import type { FolderCrumb, WorkspaceFile, WorkspaceFolder } from '../model/file'

export function FilesPage() {
  const [crumbs, setCrumbs] = useState<FolderCrumb[]>([])
  const [progress, setProgress] = useState<number | null>(null)
  const [folderModal, setFolderModal] = useState(false)
  const [folderName, setFolderName] = useState('')
  const [deleteTarget, setDeleteTarget] = useState<WorkspaceFile | null>(null)
  const [attachTarget, setAttachTarget] = useState<WorkspaceFile | null>(null)
  const [taskId, setTaskId] = useState('')
  const { notify } = useToast()
  const folderId = crumbs.at(-1)?.id
  const files = useFiles(folderId)
  const folders = useFolders(folderId)
  const upload = useUploadFile(folderId)
  const createFolder = useCreateFolder(folderId)
  const remove = useDeleteFile(folderId)
  const attach = useAttachFile()

  const enterFolder = (folder: WorkspaceFolder) => {
    setCrumbs((current) => [...current, { id: folder.id, name: folder.name }])
  }

  const uploadSelectedFile = async (file: File) => {
    setProgress(0)
    try {
      await upload.mutateAsync({ file, onProgress: setProgress })
      notify(`${file.name} uploaded`)
    } catch (error) {
      notify(error instanceof Error ? error.message : 'Upload failed')
    } finally {
      setProgress(null)
    }
  }

  const submitFolder = async () => {
    if (!folderName.trim()) return
    try {
      await createFolder.mutateAsync(folderName.trim())
      setFolderName('')
      setFolderModal(false)
      notify('Folder created')
    } catch (error) {
      notify(error instanceof Error ? error.message : 'Folder creation failed')
    }
  }

  const confirmDelete = async () => {
    if (!deleteTarget) return
    try {
      await remove.mutateAsync(deleteTarget.id)
      notify(`${deleteTarget.originalName} deleted`)
      setDeleteTarget(null)
    } catch (error) {
      notify(error instanceof Error ? error.message : 'Delete failed')
    }
  }

  const submitAttachment = async () => {
    if (!attachTarget || !taskId.trim()) return
    try {
      await attach.mutateAsync({ fileId: attachTarget.id, taskId: taskId.trim() })
      notify('File attached to task')
      setAttachTarget(null)
      setTaskId('')
    } catch (error) {
      notify(error instanceof Error ? error.message : 'Attachment failed')
    }
  }

  return (
    <section className="space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-brand-600">File manager</p>
          <h1 className="mt-1 text-3xl font-bold text-slate-950">Your files</h1>
          <p className="mt-2 text-sm text-slate-600">Upload, organize and attach documents to tasks.</p>
        </div>
        <Button onClick={() => setFolderModal(true)} variant="secondary">
          <FolderPlus size={18} />
          New folder
        </Button>
      </header>

      <FileDropzone
        disabled={upload.isPending}
        onFile={(file) => void uploadSelectedFile(file)}
        progress={progress}
      />

      <nav aria-label="Folder breadcrumb" className="flex flex-wrap items-center gap-1 text-sm">
        <button className="font-semibold text-brand-700 hover:underline" onClick={() => setCrumbs([])} type="button">
          Files
        </button>
        {crumbs.map((crumb, index) => (
          <span className="flex items-center gap-1" key={crumb.id}>
            <ChevronRight aria-hidden className="text-slate-400" size={15} />
            <button
              className="font-medium text-slate-700 hover:underline"
              onClick={() => setCrumbs((current) => current.slice(0, index + 1))}
              type="button"
            >
              {crumb.name}
            </button>
          </span>
        ))}
      </nav>

      {(files.isLoading || folders.isLoading) && <Skeleton />}
      {(files.isError || folders.isError) && (
        <ErrorState message={(files.error ?? folders.error)?.message || 'Files could not be loaded'} />
      )}
      {!files.isLoading && !folders.isLoading && !files.isError && !folders.isError && (
        <div className="overflow-hidden rounded-card border border-slate-200 bg-white shadow-card">
          {(folders.data?.length ?? 0) === 0 && (files.data?.length ?? 0) === 0 ? (
            <EmptyState message="This folder is empty" />
          ) : (
            <ul className="divide-y divide-slate-100">
              {folders.data?.map((folder) => (
                <li key={folder.id}>
                  <button
                    className="flex w-full items-center gap-3 px-4 py-4 text-left hover:bg-slate-50"
                    onClick={() => enterFolder(folder)}
                    type="button"
                  >
                    <span className="grid size-10 place-items-center rounded-xl bg-amber-50 text-amber-600">
                      <Folder size={20} />
                    </span>
                    <span className="font-semibold text-slate-800">{folder.name}</span>
                  </button>
                </li>
              ))}
              {files.data?.map((file) => (
                <li className="flex items-center gap-3 px-4 py-3" key={file.id}>
                  <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-600">
                    <FileText size={20} />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block truncate font-semibold text-slate-800">{file.originalName}</span>
                    <span className="text-xs text-slate-500">
                      {formatBytes(file.sizeBytes)} · {formatDate(file.createdAt)}
                    </span>
                  </span>
                  <div className="flex">
                    <Button aria-label={`Download ${file.originalName}`} onClick={() => void downloadFile(file)} size="icon" variant="ghost">
                      <Download size={17} />
                    </Button>
                    <Button aria-label={`Attach ${file.originalName} to task`} onClick={() => setAttachTarget(file)} size="icon" variant="ghost">
                      <Link2 size={17} />
                    </Button>
                    <Button aria-label={`Delete ${file.originalName}`} onClick={() => setDeleteTarget(file)} size="icon" variant="ghost">
                      <Trash2 size={17} />
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      <Modal onClose={() => setFolderModal(false)} open={folderModal} title="Create folder">
        <label className="text-sm font-semibold text-slate-700" htmlFor="folder-name">Folder name</label>
        <Input autoFocus className="mt-2" id="folder-name" maxLength={120} onChange={(event) => setFolderName(event.target.value)} value={folderName} />
        <div className="mt-5 flex justify-end gap-2">
          <Button onClick={() => setFolderModal(false)} variant="secondary">Cancel</Button>
          <Button disabled={!folderName.trim() || createFolder.isPending} onClick={() => void submitFolder()}>Create</Button>
        </div>
      </Modal>

      <Modal onClose={() => setDeleteTarget(null)} open={Boolean(deleteTarget)} title="Delete file?">
        <p className="text-sm leading-6 text-slate-600">
          {deleteTarget?.originalName} will be removed from your workspace.
        </p>
        <div className="mt-5 flex justify-end gap-2">
          <Button onClick={() => setDeleteTarget(null)} variant="secondary">Cancel</Button>
          <Button disabled={remove.isPending} onClick={() => void confirmDelete()}>Delete</Button>
        </div>
      </Modal>

      <Modal onClose={() => setAttachTarget(null)} open={Boolean(attachTarget)} title="Attach to task">
        <label className="text-sm font-semibold text-slate-700" htmlFor="task-id">Task ID</label>
        <Input className="mt-2" id="task-id" onChange={(event) => setTaskId(event.target.value)} placeholder="UUID from the task" value={taskId} />
        <p className="mt-2 text-xs text-slate-500">The backend stores an external link without a cross-service foreign key.</p>
        <div className="mt-5 flex justify-end gap-2">
          <Button onClick={() => setAttachTarget(null)} variant="secondary">Cancel</Button>
          <Button disabled={!taskId.trim() || attach.isPending} onClick={() => void submitAttachment()}>Attach</Button>
        </div>
      </Modal>
    </section>
  )
}

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value))
}
