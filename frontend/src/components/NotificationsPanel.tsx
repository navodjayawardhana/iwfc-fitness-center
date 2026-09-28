import { call } from '../api';
import { useLoad } from '../hooks';
import type { Report, User } from '../types';
import { btnGhost, card } from '../ui';

interface Props {
  user: User;
  report: Report;
}

export default function NotificationsPanel({ user, report }: Props) {
  const { data: inbox, reload } = useLoad(() => call<string[]>('GET', '/notifications'), [user.id], report);

  return (
    <section>
      <h2 className="mt-2 mb-3 text-xl font-semibold">Notifications</h2>
      <button className={`${btnGhost} mb-3`} onClick={reload}>
        Refresh
      </button>
      <ul className={`${card} list-disc pl-8`}>
        {(inbox ?? []).map((message, index) => (
          <li key={index} className="my-1">
            {message}
          </li>
        ))}
        {inbox && inbox.length === 0 && <li>No notifications yet. Book a session or report a fault to see one.</li>}
      </ul>
    </section>
  );
}
