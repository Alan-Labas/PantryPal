import { Moon, Sun } from 'lucide-react'
import { useTheme } from '../theme/ThemeContext.tsx'
import { iconButtonClass } from './ui.ts'

export function ThemeToggle() {
  const { resolved, setPreference } = useTheme()
  const next = resolved === 'dark' ? 'light' : 'dark'

  return (
    <button
      type="button"
      className={iconButtonClass}
      onClick={() => setPreference(next)}
      aria-label={`Switch to ${next} mode`}
      title={`Switch to ${next} mode`}
    >
      {resolved === 'dark' ? <Sun size={20} /> : <Moon size={20} />}
    </button>
  )
}
