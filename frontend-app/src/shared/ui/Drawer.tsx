import { X } from 'lucide-react'
import { useEffect, useRef, type PropsWithChildren } from 'react'

type DrawerProps = PropsWithChildren<{
  open: boolean
  title: string
  onClose: () => void
}>

export function Drawer({ children, onClose, open, title }: DrawerProps) {
  const closeButtonRef = useRef<HTMLButtonElement>(null)
  const previousFocusRef = useRef<HTMLElement | null>(null)

  useEffect(() => {
    if (!open) return
    previousFocusRef.current = document.activeElement as HTMLElement
    closeButtonRef.current?.focus()
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      previousFocusRef.current?.focus()
    }
  }, [onClose, open])

  return (
    <div className={open ? 'fixed inset-0 z-50' : 'hidden'}>
      <button aria-label="Close drawer overlay" className="absolute inset-0 bg-slate-950/35" onClick={onClose} type="button" />
      <aside aria-label={title} aria-modal="true" className="absolute inset-y-0 right-0 w-full max-w-md overflow-y-auto bg-white p-6 shadow-2xl" role="dialog">
        <div className="mb-6 flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-950">{title}</h2>
          <button ref={closeButtonRef} aria-label="Close drawer" className="rounded-lg p-2 hover:bg-slate-100" onClick={onClose} type="button"><X size={18} /></button>
        </div>
        {children}
      </aside>
    </div>
  )
}
