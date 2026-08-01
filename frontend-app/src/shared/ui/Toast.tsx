import { useCallback, useMemo, useState, type PropsWithChildren } from 'react'
import { ToastContext } from './toastContext'

export function ToastProvider({ children }: PropsWithChildren) {
  const [message, setMessage] = useState<string | null>(null)
  const notify = useCallback((nextMessage: string) => {
    setMessage(nextMessage)
    window.setTimeout(() => setMessage(null), 2500)
  }, [])
  const value = useMemo(() => ({ notify }), [notify])

  return (
    <ToastContext.Provider value={value}>
      {children}
      {message && (
        <div aria-live="polite" className="fixed bottom-5 right-5 z-50 rounded-xl bg-slate-950 px-4 py-3 text-sm text-white shadow-xl">
          {message}
        </div>
      )}
    </ToastContext.Provider>
  )
}
