import type { PropsWithChildren } from 'react'
import { cn } from '../lib/cn'

export function Badge({
  children,
  tone = 'slate',
}: PropsWithChildren<{ tone?: 'slate' | 'indigo' | 'amber' | 'emerald' }>) {
  return (
    <span
      className={cn(
        'inline-flex rounded-full px-2.5 py-1 text-xs font-semibold',
        tone === 'slate' && 'bg-slate-100 text-slate-600',
        tone === 'indigo' && 'bg-brand-50 text-brand-700',
        tone === 'amber' && 'bg-amber-50 text-amber-700',
        tone === 'emerald' && 'bg-emerald-50 text-emerald-700',
      )}
    >
      {children}
    </span>
  )
}
