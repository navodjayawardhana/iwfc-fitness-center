// Shared Tailwind class strings, so every button, field and chip looks the same.

const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-600 dark:focus-visible:outline-teal-300';

export const btn =
  `rounded-lg bg-teal-700 px-3.5 py-2 text-sm font-semibold text-white hover:bg-teal-800 cursor-pointer dark:bg-teal-400 dark:text-teal-950 dark:hover:bg-teal-300 ${focus}`;

export const btnGhost =
  `rounded-lg border border-slate-300 px-3.5 py-2 text-sm font-semibold hover:bg-slate-100 cursor-pointer dark:border-slate-600 dark:hover:bg-slate-800 ${focus}`;

export const btnDanger =
  `rounded-lg border border-red-600 px-3.5 py-2 text-sm font-semibold text-red-700 hover:bg-red-50 cursor-pointer dark:border-red-400 dark:text-red-300 dark:hover:bg-red-950 ${focus}`;

export const field =
  `min-w-0 rounded-lg border border-slate-300 bg-white px-2.5 py-2 text-sm dark:border-slate-600 dark:bg-slate-900 ${focus}`;

export const card = 'rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-900';

export const muted = 'text-slate-500 dark:text-slate-400';

const chipBase = 'inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold mr-1';

export const chip = {
  neutral: `${chipBase} bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200`,
  good: `${chipBase} bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300`,
  bad: `${chipBase} bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300`,
  warn: `${chipBase} bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300`,
  info: `${chipBase} bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300`,
} as const;
