import { X } from 'lucide-react'
import type { PropsWithChildren } from 'react'

type ModalProps = PropsWithChildren<{
  open: boolean
  title: string
  onClose: () => void
}>

export function Modal({ children, onClose, open, title }: ModalProps) {
  if (!open) return null

  return (
    <div aria-modal="true" className="fixed inset-0 z-50 grid place-items-center bg-slate-950/45 p-4" role="dialog">
      <div className="w-full max-w-lg rounded-card bg-white p-6 shadow-2xl">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-950">{title}</h2>
          <button aria-label="Close modal" className="rounded-lg p-2 hover:bg-slate-100" onClick={onClose} type="button">
            <X size={18} />
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
