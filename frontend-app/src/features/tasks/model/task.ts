export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE'

export type Task = {
  id: string
  title: string
  status: TaskStatus
  priority: TaskPriority
  dueLabel: string
  listName: string
}
