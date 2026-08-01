import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Button } from './Button'

describe('Button', () => {
  it('runs the supplied action using an accessible button', async () => {
    const user = userEvent.setup()
    const onClick = vi.fn()
    render(<Button onClick={onClick}>Create task</Button>)
    await user.click(screen.getByRole('button', { name: 'Create task' }))
    expect(onClick).toHaveBeenCalledOnce()
  })
})
