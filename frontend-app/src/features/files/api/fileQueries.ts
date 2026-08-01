import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  attachFileToTask,
  createFolder,
  deleteFile,
  getFiles,
  getFolders,
  uploadFile,
} from './fileApi'

export const fileKeys = {
  all: ['files'] as const,
  list: (folderId?: string) => [...fileKeys.all, 'list', folderId ?? 'root'] as const,
}

export const folderKeys = {
  all: ['folders'] as const,
  list: (parentId?: string) => [...folderKeys.all, 'list', parentId ?? 'root'] as const,
}

export function useFiles(folderId?: string, enabled = true) {
  return useQuery({
    queryKey: fileKeys.list(folderId),
    queryFn: () => getFiles(folderId),
    enabled,
  })
}

export function useFolders(parentId?: string) {
  return useQuery({ queryKey: folderKeys.list(parentId), queryFn: () => getFolders(parentId) })
}

export function useUploadFile(folderId?: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ file, onProgress }: { file: File; onProgress: (percent: number) => void }) =>
      uploadFile(file, folderId, onProgress),
    onSuccess: (file) => {
      queryClient.setQueryData(
        fileKeys.list(folderId),
        (current: unknown) => [file, ...((current as (typeof file)[] | undefined) ?? [])],
      )
    },
  })
}

export function useCreateFolder(parentId?: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => createFolder(name, parentId),
    onSuccess: (folder) => {
      queryClient.setQueryData(
        folderKeys.list(parentId),
        (current: unknown) => [...((current as (typeof folder)[] | undefined) ?? []), folder],
      )
    },
  })
}

export function useDeleteFile(folderId?: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteFile,
    onSuccess: (_result, fileId) => {
      queryClient.setQueryData(
        fileKeys.list(folderId),
        (current: unknown) =>
          ((current as Array<{ id: string }> | undefined) ?? []).filter((file) => file.id !== fileId),
      )
    },
  })
}

export function useAttachFile() {
  return useMutation({
    mutationFn: ({ fileId, taskId }: { fileId: string; taskId: string }) =>
      attachFileToTask(fileId, taskId),
  })
}
