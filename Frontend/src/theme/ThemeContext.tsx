import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

// 'system' follows the OS (and live-switches when the OS theme changes);
// 'light'/'dark' are manual overrides chosen with the toggle.
export type ThemePreference = 'system' | 'light' | 'dark'
type ResolvedTheme = 'light' | 'dark'

type ThemeContextValue = {
  preference: ThemePreference
  resolved: ResolvedTheme
  setPreference: (pref: ThemePreference) => void
}

const ThemeContext = createContext<ThemeContextValue | null>(null)

const systemTheme = (): ResolvedTheme =>
  window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'

const readStoredPreference = (): ThemePreference => {
  const stored = localStorage.getItem('theme')
  return stored === 'light' || stored === 'dark' ? stored : 'system'
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [preference, setPreferenceState] = useState<ThemePreference>(readStoredPreference)
  const [system, setSystem] = useState<ResolvedTheme>(systemTheme)

  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = (e: MediaQueryListEvent) => setSystem(e.matches ? 'dark' : 'light')
    media.addEventListener('change', onChange)
    return () => media.removeEventListener('change', onChange)
  }, [])

  const resolved: ResolvedTheme = preference === 'system' ? system : preference

  useEffect(() => {
    document.documentElement.dataset.theme = resolved
  }, [resolved])

  const setPreference = useCallback((pref: ThemePreference) => {
    setPreferenceState(pref)
    if (pref === 'system') {
      localStorage.removeItem('theme')
    } else {
      localStorage.setItem('theme', pref)
    }
  }, [])

  const value = useMemo(
    () => ({ preference, resolved, setPreference }),
    [preference, resolved, setPreference],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme must be used inside <ThemeProvider>')
  return ctx
}
