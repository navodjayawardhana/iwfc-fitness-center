import { useState, type FormEvent } from 'react';
import { call } from '../api';
import Drawer from './Drawer';
import { useAction, useLoad } from '../hooks';
import type { MaintenanceRequest, Report, Urgency, User } from '../types';
import { btn, btnGhost, card, chip, field, muted } from '../ui';

const URGENCIES: Urgency[] = ['LOW', 'MEDIUM', 'HIGH'];

interface Props {
  user: User;
  report: Report;
}

const urgencyChip = { LOW: chip.good, MEDIUM: chip.warn, HIGH: chip.bad } as const;
const statusChip = { PENDING: chip.info, ASSIGNED: chip.warn, COMPLETED: chip.good } as const;

export default function MaintenancePanel({ user, report }: Props) {
  const isAdmin = user.role === 'Administrator';
  const isInstructor = user.role === 'Instructor';

  // Members are deliberately allowed to try: the API answers 403 and the panel shows why.
  const path = isInstructor ? '/maintenance?mine=true' : '/maintenance';
  const { data: requests, failed, reload } = useLoad(() => call<MaintenanceRequest[]>('GET', path), [user.id], report);
  const { data: log, reload: reloadLog } = useLoad(
    () => (isAdmin ? call<string[]>('GET', '/maintenance/activity-log') : Promise.resolve<string[]>([])),
    [user.id],
    report,
  );
  const refresh = () => {
    reload();
    reloadLog();
  };
  const run = useAction(report, refresh);
  const [form, setForm] = useState({ equipmentId: 'SB-04', description: '', urgency: 'MEDIUM' as Urgency });
  const [technician, setTechnician] = useState<Record<string, string>>({});
  const [note, setNote] = useState<Record<string, string>>({});
  const [reporting, setReporting] = useState(false);

  if (failed) {
    return (
      <section>
        <h2 className="mt-2 mb-3 text-xl font-semibold">Maintenance</h2>
        <div className={`${card} border-red-600 dark:border-red-400`}>
          <h3 className="font-semibold">Access denied</h3>
          <p>{failed}</p>
          <p className={`mt-2 ${muted}`}>The maintenance log is for administrators. Switch user to continue.</p>
        </div>
      </section>
    );
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (await run(() => call('POST', '/maintenance', form), 'Fault reported')) {
      setReporting(false);
      setForm({ equipmentId: 'SB-04', description: '', urgency: 'MEDIUM' });
    }
  };

  return (
    <section>
      <div className="mt-2 mb-3 flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-xl font-semibold">Maintenance requests</h2>
        {isInstructor && (
          <button className={btn} onClick={() => setReporting(true)}>
            + Report a fault
          </button>
        )}
      </div>
      <div className="mb-4 grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-3">
        {(requests ?? []).map((request) => (
          <article className={card} key={request.id}>
            <h3 className="font-semibold">
              {request.id} · {request.equipmentId}
            </h3>
            <p>{request.description}</p>
            <p className="my-1">
              <span className={urgencyChip[request.urgency]}>{request.urgency}</span>
              <span className={statusChip[request.status]}>{request.status}</span>
            </p>
            <p className={`text-sm ${muted}`}>
              Reported by {request.reportedBy}
              {request.assignedTo && ` · Technician ${request.assignedTo}`}
            </p>
            {request.progressNotes.length > 0 && (
              <ul className="my-1 list-disc pl-5 text-sm">
                {request.progressNotes.map((entry, index) => (
                  <li key={index}>{entry}</li>
                ))}
              </ul>
            )}
            {isAdmin && request.status === 'PENDING' && (
              <div className="mt-2 flex flex-wrap items-center gap-1.5">
                <input
                  className={`${field} w-32`}
                  placeholder="Technician"
                  value={technician[request.id] ?? ''}
                  onChange={(event) => setTechnician({ ...technician, [request.id]: event.target.value })}
                />
                <button
                  className={btn}
                  onClick={() => void run(() => call('POST', `/maintenance/${request.id}/assign`, { technician: technician[request.id] }), 'Request assigned')}
                >
                  Assign
                </button>
              </div>
            )}
            {isAdmin && request.status === 'ASSIGNED' && (
              <div className="mt-2 flex flex-wrap items-center gap-1.5">
                <input
                  className={`${field} w-36`}
                  placeholder="Progress note"
                  value={note[request.id] ?? ''}
                  onChange={(event) => setNote({ ...note, [request.id]: event.target.value })}
                />
                <button
                  className={btnGhost}
                  onClick={() => void run(() => call('POST', `/maintenance/${request.id}/progress`, { note: note[request.id] }), 'Progress recorded')}
                >
                  Add note
                </button>
                <button className={btn} onClick={() => void run(() => call('POST', `/maintenance/${request.id}/complete`), 'Request completed')}>
                  Complete
                </button>
              </div>
            )}
          </article>
        ))}
        {requests && requests.length === 0 && <p className={muted}>No maintenance requests yet.</p>}
      </div>

      <Drawer open={reporting} title="Report a fault" onClose={() => setReporting(false)}>
        <form className="flex flex-col gap-3" onSubmit={(event) => void submit(event)}>
          <label className="flex flex-col gap-1 text-sm">
            Equipment ID
            <input className={field} value={form.equipmentId} onChange={(event) => setForm({ ...form, equipmentId: event.target.value })} required />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            Describe the fault
            <input className={field} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} required />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            Urgency
            <select className={field} value={form.urgency} onChange={(event) => setForm({ ...form, urgency: event.target.value as Urgency })}>
              {URGENCIES.map((urgency) => (
                <option key={urgency}>{urgency}</option>
              ))}
            </select>
          </label>
          <button className={`${btn} mt-2`} type="submit">
            Report
          </button>
        </form>
      </Drawer>

      {isAdmin && (
        <div className={card}>
          <h3 className="mb-1 font-semibold">Activity log</h3>
          <ul className="list-disc pl-5 text-sm">
            {(log ?? []).map((entry, index) => (
              <li key={index}>{entry}</li>
            ))}
            {log && log.length === 0 && <li>Nothing logged yet.</li>}
          </ul>
        </div>
      )}
    </section>
  );
}
