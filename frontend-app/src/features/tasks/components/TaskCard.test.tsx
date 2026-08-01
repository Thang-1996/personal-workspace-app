import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { Task } from '../model/task'
import { TaskCard } from './TaskCard'

const task: Task = {
  id: 'task-1',
  ownerId: 'owner-1',
  title: 'Ship the dashboard',
  status: 'TODO',
  priority: 'HIGH',
  position: 0,
  version: 0,
  tagIds: [],
  createdAt: '2026-08-01T00:00:00Z',
  updatedAt: '2026-08-01T00:00:00Z',
}

describe('TaskCard', () => {
  it('requests a DONE status when an open task is toggled', async () => {
    const user = userEvent.setup()
    const onStatusChange = vi.fn()
    render(<TaskCard onOpen={vi.fn()} onStatusChange={onStatusChange} task={task} />)

    await user.click(screen.getByRole('button', { name: 'Mark Ship the dashboard complete' }))

    expect(onStatusChange).toHaveBeenCalledWith('task-1', 'DONE')
  })
})
