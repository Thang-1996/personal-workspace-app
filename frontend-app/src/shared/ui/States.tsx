import { AlertCircle, Inbox } from 'lucide-react'

export function Skeleton() {
  return <div aria-label="Loading" className="h-20 animate-pulse rounded-card bg-slate-100" role="status" />
}

export function EmptyState({ message = 'Nothing here yet' }: { message?: string }) {
  return (
    <div className="grid place-items-center rounded-card border border-dashed border-slate-300 p-10 text-center">
      <Inbox className="mb-3 text-slate-400" />
      <p className="font-medium text-slate-700">{message}</p>
    </div>
  )
}

export function ErrorState({ message = 'Something went wrong' }: { message?: string }) {
  return (
    <div className="flex items-center gap-3 rounded-card bg-red-50 p-4 text-sm text-red-700" role="alert">
      <AlertCircle aria-hidden size={18} />
      {message}
    </div>
  )
}
