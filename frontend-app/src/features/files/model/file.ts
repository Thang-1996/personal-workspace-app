export type WorkspaceFile = {
  id: string
  originalName: string
  contentType: string
  sizeBytes: number
  checksum: string
  folderId: string | null
  status: 'READY'
  createdAt: string
}

export type WorkspaceFolder = {
  id: string
  name: string
  parentId: string | null
  createdAt: string
}

export type FolderCrumb = Pick<WorkspaceFolder, 'id' | 'name'>
