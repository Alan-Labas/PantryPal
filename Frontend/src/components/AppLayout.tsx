import { NavLink, Outlet } from 'react-router-dom'
import {
  BarChart3,
  BookOpen,
  Home,
  LayoutDashboard,
  PackageOpen,
  Sparkles,
} from 'lucide-react'
import { navLinkActive, navLinkClass, navLinkIdle } from './ui.ts'

const navItems = [
  { to: '/', label: 'Nadzorna plošča', icon: LayoutDashboard, end: true },
  { to: '/zaloga', label: 'Zaloga', icon: PackageOpen, end: false },
  { to: '/recepti', label: 'Recepti', icon: BookOpen, end: false },
  { to: '/odprti-podatki', label: 'Odprti podatki', icon: BarChart3, end: false },
  { to: '/ai-asistent', label: 'AI Asistent', icon: Sparkles, end: false },
]

const linkClasses = ({ isActive }: { isActive: boolean }) =>
  `${navLinkClass} ${isActive ? navLinkActive : navLinkIdle}`

export function AppLayout() {
  const links = navItems.map(({ to, label, icon: Icon, end }) => (
    <NavLink key={to} to={to} end={end} className={linkClasses}>
      <Icon size={20} className="shrink-0" />
      {label}
    </NavLink>
  ))

  return (
    <div className="flex min-h-screen">
      <aside className="sticky top-0 hidden h-screen w-64 flex-col border-r border-border bg-surface px-3.5 py-5 md:flex">
        <div className="flex items-center gap-2.5 px-3 pb-5 text-[17px] font-bold text-primary">
          <Home size={22} />
          PantryPal
        </div>
        <nav aria-label="Main navigation" className="flex flex-1 flex-col gap-1">
          {links}
        </nav>
        <div className="border-t border-border px-3 pt-3 text-xs text-muted">
          TIDS – Vaje (Naloge 1–4)
        </div>
      </aside>

      <main className="mx-auto w-full max-w-5xl flex-1 px-5 pb-22 pt-6 md:p-8">
        <Outlet />
      </main>

      <nav
        className="fixed bottom-0 left-0 right-0 z-50 flex justify-around border-t border-border bg-surface px-2 pt-1.5 pb-[calc(6px+env(safe-area-inset-bottom))] md:hidden"
        aria-label="Main navigation"
      >
        {navItems.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `flex min-w-14 flex-col items-center gap-0.5 rounded-lg px-1 py-1.5 text-[11px] font-medium no-underline transition-colors duration-200 hover:no-underline ${
                isActive ? navLinkActive : navLinkIdle
              }`
            }
          >
            <Icon size={20} />
            {label.split(' ')[0]}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
