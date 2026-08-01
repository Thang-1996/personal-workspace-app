import { httpClient } from '../../../shared/api/httpClient'
import type { WorkspaceFile, WorkspaceFolder } from '../model/file'

export async function getFiles(folderId?: string): Promise<WorkspaceFile[]> {
  const response = await httpClient.get<WorkspaceFile[]>('/v1/files', {
    params: folderId ? { folderId } : undefined,
  })
  return response.data
}

export async function getFolders(parentId?: string): Promise<WorkspaceFolder[]> {
  const response = await httpClient.get<WorkspaceFolder[]>('/v1/folders', {
    params: parentId ? { parentId } : undefined,
  })
  return response.data
}

export async function uploadFile(
  file: File,
  folderId: string | undefined,
  onProgress: (percent: number) => void,
): Promise<WorkspaceFile> {
  const body = new FormData()
  body.append('file', file)
  const response = await httpClient.post<WorkspaceFile>('/v1/files/upload', body, {
    params: folderId ? { folderId } : undefined,
    onUploadProgress: ({ loaded, total }) => {
      if (total) onProgress(Math.round((loaded * 100) / total))
    },
  })
  return response.data
}

export async function createFolder(name: string, parentId?: string): Promise<WorkspaceFolder> {
  const response = await httpClient.post<WorkspaceFolder>('/v1/folders', { name, parentId })
  return response.data
}

export async function deleteFile(fileId: string): Promise<void> {
  await httpClient.delete(`/v1/files/${fileId}`)
}

export async function attachFileToTask(fileId: string, taskId: string): Promise<void> {
  await httpClient.post(`/v1/files/${fileId}/links/tasks/${taskId}`)
}

export async function downloadFile(file: WorkspaceFile): Promise<void> {
  const response = await httpClient.get<Blob>(`/v1/files/${file.id}/download`, {
    responseType: 'blob',
  })
  const url = URL.createObjectURL(response.data)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = file.originalName
  document.body.append(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}
