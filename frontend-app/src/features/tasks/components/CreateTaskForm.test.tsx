import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { CreateTaskForm } from './CreateTaskForm'

describe('CreateTaskForm', () => {
  it('validates a short title before submission', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    render(<CreateTaskForm onCancel={vi.fn()} onSubmit={onSubmit} />)
    await user.type(screen.getByLabelText('Task title'), 'x')
    await user.click(screen.getByRole('button', { name: 'Create task' }))
    expect(await screen.findByText('Title must contain at least 3 characters')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })
})
