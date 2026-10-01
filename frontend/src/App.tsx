import { useCallback, useEffect, useState, type ReactNode } from 'react';
import { call, describeError, setSessionExpiredHandler, signOut, tokenStore } from './api';
import DashboardPanel, { type DashboardTarget } from './components/DashboardPanel';
import EquipmentPanel from './components/EquipmentPanel';
import LoginScreen from './components/LoginScreen';
import MaintenancePanel from './components/MaintenancePanel';
import NotificationsPanel from './components/NotificationsPanel';
import SessionsPanel from './components/SessionsPanel';
import UsersPanel from './components/UsersPanel';
import type { Message, User } from './types';
import { brandText, btnGhost, muted } from './ui';

type TabId = 'dashboard' | DashboardTarget | 'users';

/** Each section lists the roles that may open it; no list means everyone. The API enforces the same rules. */
const TABS: { id: TabId; label: string; roles?: User['role'][] }[] = [
  { id: 'dashboard', label: 'Dashboard' },
  { id: 'equipment', label: 'Equipment' },
  { id: 'sessions', label: 'Sessions' },
  { id: 'maintenance', label: 'Maintenance', roles: ['Administrator', 'Instructor'] },
  { id: 'notifications', label: 'Notifications' },
  { id: 'users', label: 'Users', roles: ['Administrator'] },
];

/** Small inline icons so the sidebar reads like an admin console. */
const ICONS: Record<TabId, ReactNode> = {
  dashboard: (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect x="3" y="3" width="7" height="9" rx="1.5" /><rect x="14" y="3" width="7" height="5" rx="1.5" />
      <rect x="14" y="12" width="7" height="9" rx="1.5" /><rect x="3" y="16" width="7" height="5" rx="1.5" />
    </svg>
  ),
  equipment: (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M6.5 6.5v11M17.5 6.5v11M3 9v6M21 9v6M6.5 12h11" />
    </svg>
  ),
  sessions: (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect x="3" y="4" width="18" height="17" rx="2" /><path d="M3 9h18M8 2v4M16 2v4" />
    </svg>
  ),
  maintenance: (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M14.7 6.3a4.5 4.5 0 0 0-6 5.7L3 17.7V21h3.3l5.7-5.7a4.5 4.5 0 0 0 5.7-6l-3 3-2.8-.7-.7-2.8 3-3Z" />
    </svg>
  ),
  notifications: (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M18 9a6 6 0 1 0-12 0c0 6-2 7-2 7h16s-2-1-2-7M10.3 20a2 2 0 0 0 3.4 0" />
    </svg>
  ),
  users: (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <circle cx="9" cy="8" r="3.5" /><path d="M2.5 20a6.5 6.5 0 0 1 13 0M16 4.8a3.5 3.5 0 0 1 0 6.4M17.7 14.1a6.5 6.5 0 0 1 3.8 5.9" />
    </svg>
  ),
};

function initialsOf(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('');
}

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [checking, setChecking] = useState(() => tokenStore.get() !== null);
  const [tab, setTab] = useState<TabId>('dashboard');
  const [message, setMessage] = useState<Message | null>(null);

  const report = useCallback((next: Message | null) => setMessage(next), []);

  // A page reload keeps the token, so ask the API who it belongs to before showing the login screen.
  useEffect(() => {
    if (!checking) return;
    call<User>('GET', '/me')
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setChecking(false));
  }, [checking]);

  // If the API ever says the token is no longer valid, go back to the login screen.
  useEffect(() => {
    setSessionExpiredHandler(() => {
      setUser(null);
      setMessage({ kind: 'error', text: 'Your session has ended. Please sign in again.' });
    });
    return () => setSessionExpiredHandler(null);
  }, []);

  const leave = async () => {
    await signOut().catch((error: unknown) => report({ kind: 'error', text: describeError(error) }));
    setUser(null);
    setMessage(null);
    setTab('dashboard');
  };

  if (checking) {
    return <p className={`p-6 ${muted}`}>Checking your session…</p>;
  }

  if (!user) {
    return (
      <>
        {message && (
          <p role="status" className="mx-auto mt-4 max-w-md rounded-xl bg-red-100 px-3.5 py-2.5 text-sm text-red-900 dark:bg-red-950 dark:text-red-200">
            {message.text}
          </p>
        )}
        <LoginScreen onSignedIn={setUser} />
      </>
    );
  }

  const visibleTabs = TABS.filter((item) => !item.roles || item.roles.includes(user.role));
  const current = TABS.find((item) => item.id === tab);

  const open = (next: TabId) => {
    setTab(next);
    setMessage(null);
  };

  return (
    <div className="min-h-screen">
      {/* ---- sidebar (desktop) ------------------------------------------------------------- */}
      <aside className="fixed inset-y-0 left-0 z-20 hidden w-64 flex-col border-r border-slate-200/70 bg-white/80 backdrop-blur-md lg:flex dark:border-slate-800/80 dark:bg-slate-950/70">
        <div className="flex items-center gap-2.5 px-5 py-5">
          <img src="/logo.svg" alt="" className="h-8 w-auto" />
          <span className="text-xl font-extrabold tracking-tight">
            Fit<span className={brandText}>Pulse</span>
          </span>
        </div>

        <nav className="flex flex-1 flex-col gap-1 px-3" aria-label="Sections">
          {visibleTabs.map((item) => (
            <button
              key={item.id}
              onClick={() => open(item.id)}
              className={`flex cursor-pointer items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-medium transition ${
                item.id === tab
                  ? 'bg-gradient-to-r from-teal-600 to-emerald-500 text-white shadow-md shadow-teal-600/25'
                  : 'text-slate-600 hover:bg-teal-600/10 hover:text-teal-700 dark:text-slate-300 dark:hover:bg-teal-300/10 dark:hover:text-teal-300'
              }`}
            >
              {ICONS[item.id]}
              {item.label}
            </button>
          ))}
        </nav>

        <div className="border-t border-slate-200/70 px-4 py-4 dark:border-slate-800/80">
          <div className="mb-3 flex items-center gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-teal-600 to-emerald-500 text-sm font-bold text-white">
              {initialsOf(user.name)}
            </span>
            <span className="min-w-0">
              <span className="block truncate text-sm font-semibold">{user.name}</span>
              <span className={`block text-xs ${muted}`}>{user.role}</span>
            </span>
          </div>
          <button className={`${btnGhost} w-full`} onClick={() => void leave()}>
            Sign out
          </button>
        </div>
      </aside>

      {/* ---- content ----------------------------------------------------------------------- */}
      <div className="lg:pl-64">
        <header className="sticky top-0 z-10 border-b border-slate-200/70 bg-white/75 backdrop-blur-md dark:border-slate-800/80 dark:bg-slate-950/70">
          <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 lg:px-8">
            <div className="flex items-center gap-2.5">
              <img src="/logo.svg" alt="" className="h-8 w-auto lg:hidden" />
              <h1 className="text-lg font-bold tracking-tight">{current?.label}</h1>
            </div>
            <div className="flex items-center gap-3 text-sm lg:hidden">
              <span>
                {user.name} <span className={muted}>({user.role})</span>
              </span>
              <button className={btnGhost} onClick={() => void leave()}>
                Sign out
              </button>
            </div>
            <p className={`hidden text-xs lg:block ${muted}`}>Intelligent Wellness and Fitness Center</p>
          </div>

          {/* tab pills on small screens, where there is no sidebar */}
          <nav className="flex gap-1.5 overflow-x-auto px-4 pb-3 lg:hidden" aria-label="Sections">
            {visibleTabs.map((item) => (
              <button
                key={item.id}
                onClick={() => open(item.id)}
                className={`shrink-0 cursor-pointer rounded-full px-4 py-2 text-sm font-medium transition ${
                  item.id === tab
                    ? 'bg-gradient-to-r from-teal-600 to-emerald-500 text-white shadow-md shadow-teal-600/25'
                    : 'text-slate-600 hover:bg-teal-600/10 hover:text-teal-700 dark:text-slate-300 dark:hover:bg-teal-300/10 dark:hover:text-teal-300'
                }`}
              >
                {item.label}
              </button>
            ))}
          </nav>
        </header>

        <div className="px-4 pt-4 pb-12 lg:px-8">
          {message && (
            <div
              role="status"
              className={`mb-4 flex items-center justify-between gap-3 rounded-xl px-3.5 py-2.5 text-sm ${
                message.kind === 'ok'
                  ? 'bg-emerald-100 text-emerald-900 dark:bg-emerald-950 dark:text-emerald-200'
                  : 'bg-red-100 text-red-900 dark:bg-red-950 dark:text-red-200'
              }`}
            >
              <span>{message.text}</span>
              <button onClick={() => setMessage(null)} aria-label="Dismiss" className="cursor-pointer px-1.5 text-lg leading-none">
                ×
              </button>
            </div>
          )}

          <main>
            {tab === 'dashboard' && <DashboardPanel user={user} report={report} goTo={open} />}
            {tab === 'equipment' && <EquipmentPanel user={user} report={report} />}
            {tab === 'sessions' && <SessionsPanel user={user} report={report} />}
            {tab === 'maintenance' && <MaintenancePanel user={user} report={report} />}
            {tab === 'notifications' && <NotificationsPanel user={user} report={report} />}
            {tab === 'users' && <UsersPanel user={user} report={report} />}
          </main>
        </div>
      </div>
    </div>
  );
}
