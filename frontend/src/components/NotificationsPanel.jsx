import { call } from '../api.js';
import { useLoad } from '../hooks.js';

export default function NotificationsPanel({ user, report }) {
  const { data: inbox, reload } = useLoad(() => call('GET', '/notifications', user.id), [user.id], report);

  return (
    <section>
      <h2>Notifications</h2>
      <button className="ghost" onClick={reload}>
        Refresh
      </button>
      <ul className="card notes">
        {(inbox ?? []).map((message, index) => (
          <li key={index}>{message}</li>
        ))}
        {inbox && inbox.length === 0 && <li>No notifications yet. Book a session or report a fault to see one.</li>}
      </ul>
    </section>
  );
}
