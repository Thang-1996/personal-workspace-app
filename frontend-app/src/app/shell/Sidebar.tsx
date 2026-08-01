import { CheckSquare2, Files, LayoutDashboard, MessageSquare, Search, X } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { useUiStore } from '../store/uiStore'
import { cn } from '../../shared/lib/cn'

const links = [
  { to: '/tasks', label: 'My tasks', icon: CheckSquare2 },
  { to: '/files', label: 'Files', icon: Files },
  { to: '/chat', label: 'Chat', icon: MessageSquare },
  { to: '/search', label: 'Search', icon: Search },
]

export function Sidebar() {
  const sidebarOpen = useUiStore((state) => state.sidebarOpen)
  const closeSidebar = useUiStore((state) => state.closeSidebar)

  return (
    <>
      {sidebarOpen && (
        <button
          aria-label="Close navigation overlay"
          className="fixed inset-0 z-30 bg-slate-950/35 lg:hidden"
          onClick={closeSidebar}
          type="button"
        />
      )}
      <aside
        aria-label="Primary navigation"
        className={cn(
          'fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-slate-200 bg-white p-4 transition-transform lg:translate-x-0',
          sidebarOpen ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="mb-8 flex h-12 items-center gap-3 px-2">
          <span className="grid size-9 place-items-center rounded-xl bg-brand-600 text-white">
            <LayoutDashboard size={20} />
          </span>
          <div>
            <p className="font-bold text-slate-950">Workspace</p>
            <p className="text-xs text-slate-500">Personal command center</p>
          </div>
          <button
            aria-label="Close navigation"
            className="ml-auto rounded-lg p-2 text-slate-500 hover:bg-slate-100 lg:hidden"
            onClick={closeSidebar}
            type="button"
          >
            <X size={18} />
          </button>
        </div>
        <nav className="space-y-1">
          {links.map(({ to, label, icon: Icon }) => (
            <NavLink
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-brand-50 text-brand-700'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950',
                )
              }
              key={to}
              onClick={closeSidebar}
              to={to}
            >
              <Icon aria-hidden size={19} />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-auto rounded-card bg-slate-950 p-4 text-white">
          <p className="text-sm font-semibold">Make today count</p>
          <p className="mt-1 text-xs leading-5 text-slate-300">Three focused tasks are waiting for you.</p>
        </div>
      </aside>
    </>
  )
}
