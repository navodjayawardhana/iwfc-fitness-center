import { call } from '../api';
import { useAction, useLoad } from '../hooks';
import type { Report, User } from '../types';
import { btnGhost, card } from '../ui';

interface Props {
  user: User;
  report: Report;
}

export default function NotificationsPanel({ user, report }: Props) {
  const { data: inbox, reload } = useLoad(() => call<string[]>('GET', '/notifications'), [user.id], report);

  const run = useAction(report, reload);
  const isStaff = user.role !== 'Member';

  const sendReminders = () =>
    run(async () => {
      const result = await call<{ sent: number }>('POST', '/reminders');
      report({ kind: 'ok', text: `Reminders sent: ${result.sent}` });
    });

  return (
    <section>
      <h2 className="mt-2 mb-3 text-xl font-semibold">Notifications</h2>
      <div className="mb-3 flex flex-wrap gap-2">
        <button className={btnGhost} onClick={reload}>
          Refresh
        </button>
        {isStaff && (
          <button className={btnGhost} onClick={() => void sendReminders()}>
            Send session reminders (next 24h)
          </button>
        )}
      </div>
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
