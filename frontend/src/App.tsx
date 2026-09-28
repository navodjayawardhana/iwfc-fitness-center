import { useCallback, useEffect, useState } from 'react';
import { call, describeError, setSessionExpiredHandler, signOut, tokenStore } from './api';
import EquipmentPanel from './components/EquipmentPanel';
import LoginScreen from './components/LoginScreen';
import MaintenancePanel from './components/MaintenancePanel';
import NotificationsPanel from './components/NotificationsPanel';
import SessionsPanel from './components/SessionsPanel';
import UsersPanel from './components/UsersPanel';
import type { Message, User } from './types';
import { btnGhost, muted } from './ui';

type TabId = 'equipment' | 'sessions' | 'maintenance' | 'notifications' | 'users';

const TABS: { id: TabId; label: string; adminOnly?: boolean }[] = [
  { id: 'equipment', label: 'Equipment' },
  { id: 'sessions', label: 'Sessions' },
  { id: 'maintenance', label: 'Maintenance' },
  { id: 'notifications', label: 'Notifications' },
  { id: 'users', label: 'Users', adminOnly: true },
];

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [checking, setChecking] = useState(() => tokenStore.get() !== null);
  const [tab, setTab] = useState<TabId>('sessions');
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
    setTab('sessions');
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

  const visibleTabs = TABS.filter((item) => !item.adminOnly || user.role === 'Administrator');

  return (
    <div className="mx-auto max-w-5xl px-4 pb-12">
      <header className="flex flex-wrap items-center justify-between gap-3 pt-5 pb-3">
        <div className="flex items-center gap-3">
          <img src="/logo.svg" alt="" className="h-10 w-10" />
          <div>
            <h1 className="text-2xl font-bold tracking-wide">FitPulse</h1>
            <p className={muted}>Intelligent Wellness and Fitness Center</p>
          </div>
        </div>
        <div className="flex items-center gap-3 text-sm">
          <span>
            {user.name} <span className={muted}>({user.role})</span>
          </span>
          <button className={btnGhost} onClick={() => void leave()}>
            Sign out
          </button>
        </div>
      </header>

      <nav className="mb-4 flex flex-wrap gap-1 border-b border-slate-200 dark:border-slate-700" aria-label="Sections">
        {visibleTabs.map((item) => (
          <button
            key={item.id}
            onClick={() => {
              setTab(item.id);
              setMessage(null);
            }}
            className={`-mb-px cursor-pointer border-b-[3px] px-3.5 py-2.5 text-sm ${
              item.id === tab
                ? 'border-teal-600 font-semibold dark:border-teal-300'
                : `border-transparent ${muted} hover:text-slate-900 dark:hover:text-slate-100`
            }`}
          >
            {item.label}
          </button>
        ))}
      </nav>

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
        {tab === 'equipment' && <EquipmentPanel user={user} report={report} />}
        {tab === 'sessions' && <SessionsPanel user={user} report={report} />}
        {tab === 'maintenance' && <MaintenancePanel user={user} report={report} />}
        {tab === 'notifications' && <NotificationsPanel user={user} report={report} />}
        {tab === 'users' && <UsersPanel user={user} report={report} />}
      </main>
    </div>
  );
}
