import { useCallback, useEffect, useState } from 'react';
import { DEMO_USERS, call, describeError } from './api.js';
import EquipmentPanel from './components/EquipmentPanel.jsx';
import SessionsPanel from './components/SessionsPanel.jsx';
import MaintenancePanel from './components/MaintenancePanel.jsx';
import NotificationsPanel from './components/NotificationsPanel.jsx';

const TABS = [
  { id: 'equipment', label: 'Equipment' },
  { id: 'sessions', label: 'Sessions' },
  { id: 'maintenance', label: 'Maintenance' },
  { id: 'notifications', label: 'Notifications' },
];

export default function App() {
  const [userId, setUserId] = useState('M-1');
  const [user, setUser] = useState(null);
  const [tab, setTab] = useState('sessions');
  const [message, setMessage] = useState(null);

  const report = useCallback((next) => setMessage(next), []);

  useEffect(() => {
    setMessage(null);
    call('POST', '/login', null, { userId })
      .then(setUser)
      .catch((error) => report({ kind: 'error', text: describeError(error) }));
  }, [userId, report]);

  return (
    <div className="app">
      <header className="topbar">
        <div>
          <h1>IWFC</h1>
          <p>Intelligent Wellness and Fitness Center</p>
        </div>
        <label className="switcher">
          Signed in as
          <select value={userId} onChange={(event) => setUserId(event.target.value)}>
            {DEMO_USERS.map((demo) => (
              <option key={demo.id} value={demo.id}>
                {demo.label}
              </option>
            ))}
          </select>
        </label>
      </header>

      <nav className="tabs" aria-label="Sections">
        {TABS.map((item) => (
          <button
            key={item.id}
            className={item.id === tab ? 'tab active' : 'tab'}
            onClick={() => {
              setTab(item.id);
              setMessage(null);
            }}
          >
            {item.label}
          </button>
        ))}
      </nav>

      {message && (
        <div className={`banner ${message.kind}`} role="status">
          <span>{message.text}</span>
          <button onClick={() => setMessage(null)} aria-label="Dismiss">
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
