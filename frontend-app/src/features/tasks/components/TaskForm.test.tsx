import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { TaskForm } from './TaskForm'

describe('TaskForm', () => {
  it('validates a short title before submission', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    render(<TaskForm onCancel={vi.fn()} onSubmit={onSubmit} taskLists={[]} />)
    await user.type(screen.getByLabelText('Task title'), 'x')
    await user.click(screen.getByRole('button', { name: 'Create task' }))
    expect(await screen.findByText('Title must contain at least 3 characters')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })
})
