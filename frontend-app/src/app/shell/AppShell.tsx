import { Menu, Search } from 'lucide-react'
import { Outlet } from 'react-router-dom'
import { useUiStore } from '../store/uiStore'
import { Button } from '../../shared/ui/Button'
import { Sidebar } from './Sidebar'

export function AppShell() {
  const toggleSidebar = useUiStore((state) => state.toggleSidebar)

  return (
    <div className="min-h-screen bg-slate-50">
      <Sidebar />
      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b border-slate-200 bg-white/95 px-4 backdrop-blur sm:px-6">
          <Button
            aria-label="Open navigation"
            className="lg:hidden"
            onClick={toggleSidebar}
            size="icon"
            variant="ghost"
          >
            <Menu size={20} />
          </Button>
          <label className="relative max-w-lg flex-1">
            <span className="sr-only">Search workspace</span>
            <Search className="pointer-events-none absolute left-3 top-2.5 text-slate-400" size={18} />
            <input
              className="h-10 w-full rounded-xl border border-slate-200 bg-slate-50 pl-10 pr-3 text-sm placeholder:text-slate-400 hover:border-slate-300"
              placeholder="Search tasks, files, messages…"
              type="search"
            />
          </label>
          <button
            aria-label="Open user profile"
            className="grid size-10 place-items-center rounded-full bg-brand-100 text-sm font-bold text-brand-700"
            type="button"
          >
            MT
          </button>
        </header>
        <main className="mx-auto max-w-7xl p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
