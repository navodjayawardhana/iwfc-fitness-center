import { useCallback, useEffect, useState } from 'react';
import { DEMO_USERS, call, describeError } from './api';
import EquipmentPanel from './components/EquipmentPanel';
import SessionsPanel from './components/SessionsPanel';
import MaintenancePanel from './components/MaintenancePanel';
import NotificationsPanel from './components/NotificationsPanel';
import type { Message, User } from './types';
import { field, muted } from './ui';

type TabId = 'equipment' | 'sessions' | 'maintenance' | 'notifications';

const TABS: { id: TabId; label: string }[] = [
  { id: 'equipment', label: 'Equipment' },
  { id: 'sessions', label: 'Sessions' },
  { id: 'maintenance', label: 'Maintenance' },
  { id: 'notifications', label: 'Notifications' },
];

export default function App() {
  const [userId, setUserId] = useState('M-1');
  const [user, setUser] = useState<User | null>(null);
  const [tab, setTab] = useState<TabId>('sessions');
  const [message, setMessage] = useState<Message | null>(null);

  const report = useCallback((next: Message | null) => setMessage(next), []);

  useEffect(() => {
    setMessage(null);
    call<User>('POST', '/login', null, { userId })
      .then(setUser)
      .catch((error: unknown) => report({ kind: 'error', text: describeError(error) }));
  }, [userId, report]);

  return (
    <div className="mx-auto max-w-5xl px-4 pb-12">
      <header className="flex flex-wrap items-center justify-between gap-3 pt-5 pb-3">
        <div>
          <h1 className="text-2xl font-bold tracking-wide">IWFC</h1>
          <p className={muted}>Intelligent Wellness and Fitness Center</p>
        </div>
        <label className={`flex flex-col gap-1 text-sm ${muted}`}>
          Signed in as
          <select className={field} value={userId} onChange={(event) => setUserId(event.target.value)}>
            {DEMO_USERS.map((demo) => (
              <option key={demo.id} value={demo.id}>
                {demo.label}
              </option>
            ))}
          </select>
        </label>
      </header>

      <nav className="mb-4 flex flex-wrap gap-1 border-b border-slate-200 dark:border-slate-700" aria-label="Sections">
        {TABS.map((item) => (
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

      {user && (
        <main>
          {tab === 'equipment' && <EquipmentPanel user={user} report={report} />}
          {tab === 'sessions' && <SessionsPanel user={user} report={report} />}
          {tab === 'maintenance' && <MaintenancePanel user={user} report={report} />}
          {tab === 'notifications' && <NotificationsPanel user={user} report={report} />}
        </main>
      )}
    </div>
  );
}
