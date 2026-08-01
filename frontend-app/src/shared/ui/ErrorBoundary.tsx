import { Component, type ErrorInfo, type PropsWithChildren } from 'react'
import { ErrorState } from './States'

type State = { hasError: boolean }

export class ErrorBoundary extends Component<PropsWithChildren, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Unhandled application error', error, info)
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="mx-auto max-w-xl p-8">
          <ErrorState message="The application could not render. Please refresh and try again." />
        </main>
      )
    }

    return this.props.children
  }
}
