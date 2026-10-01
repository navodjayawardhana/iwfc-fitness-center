// Shared Tailwind class strings, so every button, field and chip looks the same.
// The palette follows the FitPulse logo: deep teal -> mint gradient on navy ink.

const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-600 dark:focus-visible:outline-teal-300';

export const btn =
  `rounded-xl bg-gradient-to-r from-teal-600 to-emerald-500 px-4 py-2 text-sm font-semibold text-white shadow-md shadow-teal-600/25 transition hover:from-teal-500 hover:to-emerald-400 hover:shadow-lg hover:shadow-teal-500/30 active:scale-[.98] disabled:opacity-60 cursor-pointer ${focus}`;

export const btnGhost =
  `rounded-xl border border-slate-300/80 bg-white/70 px-4 py-2 text-sm font-semibold backdrop-blur transition hover:border-teal-400 hover:text-teal-700 cursor-pointer dark:border-slate-600 dark:bg-slate-900/60 dark:hover:border-teal-500 dark:hover:text-teal-300 ${focus}`;

export const btnDanger =
  `rounded-xl border border-red-600/70 px-4 py-2 text-sm font-semibold text-red-700 transition hover:bg-red-50 cursor-pointer dark:border-red-400 dark:text-red-300 dark:hover:bg-red-950 ${focus}`;

export const field =
  `min-w-0 rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm transition focus:border-teal-500 dark:border-slate-600 dark:bg-slate-900 ${focus}`;

export const card =
  'rounded-2xl border border-slate-200/80 bg-white/80 p-5 shadow-sm backdrop-blur dark:border-slate-700/80 dark:bg-slate-900/70';

export const muted = 'text-slate-500 dark:text-slate-400';

/** The brand wordmark gradient, for text like the "Pulse" in FitPulse. */
export const brandText = 'bg-gradient-to-r from-teal-600 to-mint-400 bg-clip-text text-transparent dark:from-teal-300 dark:to-mint-300';

const chipBase = 'inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold mr-1';

export const chip = {
  neutral: `${chipBase} bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200`,
  good: `${chipBase} bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300`,
  bad: `${chipBase} bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300`,
  warn: `${chipBase} bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300`,
  info: `${chipBase} bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300`,
} as const;
