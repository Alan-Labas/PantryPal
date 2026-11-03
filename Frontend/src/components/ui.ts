// Shared Tailwind class strings so forms and buttons look identical everywhere.

export const inputClass =
  'w-full min-w-0 min-h-11 px-3.5 py-2.5 rounded-lg border border-border bg-surface text-fg text-base ' +
  'transition-colors duration-200 focus:outline-none focus:border-primary focus:ring-[3px] focus:ring-primary-soft'

export const labelClass = 'text-sm font-medium text-muted'

export const buttonClass =
  'inline-flex w-full items-center justify-center gap-2 min-h-11 px-4 py-2.5 rounded-lg border-none ' +
  'bg-primary text-white text-base font-semibold cursor-pointer transition-colors duration-200 ' +
  'hover:bg-primary-hover focus-visible:outline-2 focus-visible:outline-primary focus-visible:outline-offset-2 ' +
  'disabled:opacity-60 disabled:cursor-not-allowed'

export const secondaryButtonClass =
  'inline-flex items-center justify-center gap-2 min-h-11 px-4 py-2.5 rounded-lg border border-border ' +
  'bg-surface text-fg text-sm font-semibold cursor-pointer transition-colors duration-200 ' +
  'hover:bg-surface-hover focus-visible:outline-2 focus-visible:outline-primary focus-visible:outline-offset-2 ' +
  'disabled:opacity-60 disabled:cursor-not-allowed'

export const iconButtonClass =
  'inline-flex h-11 w-11 items-center justify-center rounded-lg border border-border bg-surface text-muted ' +
  'cursor-pointer transition-colors duration-200 hover:bg-surface-hover hover:text-fg ' +
  'focus-visible:outline-2 focus-visible:outline-primary focus-visible:outline-offset-2'

export const cardClass = 'rounded-xl border border-border bg-surface p-5 shadow-card'

export const navLinkClass =
  'flex items-center gap-3 min-h-11 px-3 py-2.5 rounded-lg text-[15px] font-medium cursor-pointer ' +
  'transition-colors duration-200 no-underline hover:no-underline'

export const navLinkIdle = 'text-muted hover:bg-surface-hover hover:text-fg'
export const navLinkActive = 'bg-primary-soft text-primary'
