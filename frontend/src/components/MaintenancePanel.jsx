import { useState } from 'react';
import { call } from '../api.js';
import { useAction, useLoad } from '../hooks.js';

const URGENCIES = ['LOW', 'MEDIUM', 'HIGH'];

export default function MaintenancePanel({ user, report }) {
  const isAdmin = user.role === 'Administrator';
  const isInstructor = user.role === 'Instructor';

  // Members are deliberately allowed to try: the API answers 403 and the panel shows why.
  const path = isInstructor ? '/maintenance?mine=true' : '/maintenance';
  const { data: requests, failed, reload } = useLoad(() => call('GET', path, user.id), [user.id], report);
  const { data: log, reload: reloadLog } = useLoad(
    () => (isAdmin ? call('GET', '/maintenance/activity-log', user.id) : Promise.resolve([])),
    [user.id],
    report,
  );
  const refresh = () => {
    reload();
    reloadLog();
  };
  const run = useAction(report, refresh);
  const [form, setForm] = useState({ equipmentId: 'SB-04', description: '', urgency: 'MEDIUM' });
  const [technician, setTechnician] = useState({});
  const [note, setNote] = useState({});

  if (failed) {
    return (
      <section>
        <h2>Maintenance</h2>
        <div className="card denied">
          <h3>Access denied</h3>
          <p>{failed}</p>
          <p className="hint">The maintenance log is for administrators. Switch user to continue.</p>
        </div>
      </section>
    );
  }

  return (
    <section>
      <h2>Maintenance requests</h2>
      <div className="grid">
        {(requests ?? []).map((request) => (
          <article className="card" key={request.id}>
            <h3>
              {request.id} · {request.equipmentId}
            </h3>
            <p>{request.description}</p>
            <p>
              <span className={`chip urgency-${request.urgency.toLowerCase()}`}>{request.urgency}</span>{' '}
              <span className={`chip status-${request.status.toLowerCase()}`}>{request.status}</span>
            </p>
            <p className="meta">
              Reported by {request.reportedBy}
              {request.assignedTo && ` · Technician ${request.assignedTo}`}
            </p>
            {request.progressNotes.length > 0 && (
              <ul className="notes">
                {request.progressNotes.map((entry, index) => (
                  <li key={index}>{entry}</li>
                ))}
              </ul>
            )}
            {isAdmin && request.status === 'PENDING' && (
              <div className="actions">
                <input
                  placeholder="Technician"
                  value={technician[request.id] ?? ''}
                  onChange={(event) => setTechnician({ ...technician, [request.id]: event.target.value })}
                />
                <button
                  onClick={() =>
                    run(() => call('POST', `/maintenance/${request.id}/assign`, user.id, { technician: technician[request.id] }), 'Request assigned')
                  }
                >
                  Assign
                </button>
              </div>
            )}
            {isAdmin && request.status === 'ASSIGNED' && (
              <div className="actions">
                <input
                  placeholder="Progress note"
                  value={note[request.id] ?? ''}
                  onChange={(event) => setNote({ ...note, [request.id]: event.target.value })}
                />
                <button
                  className="ghost"
                  onClick={() => run(() => call('POST', `/maintenance/${request.id}/progress`, user.id, { note: note[request.id] }), 'Progress recorded')}
                >
                  Add note
                </button>
                <button onClick={() => run(() => call('POST', `/maintenance/${request.id}/complete`, user.id), 'Request completed')}>
                  Complete
                </button>
              </div>
            )}
          </article>
        ))}
        {requests && requests.length === 0 && <p className="hint">No maintenance requests yet.</p>}
      </div>

      {isInstructor && (
        <form
          className="card form"
          onSubmit={(event) => {
            event.preventDefault();
            run(() => call('POST', '/maintenance', user.id, form), 'Fault reported');
          }}
        >
          <h3>Report a fault</h3>
          <input placeholder="Equipment ID" value={form.equipmentId} onChange={(event) => setForm({ ...form, equipmentId: event.target.value })} />
          <input placeholder="Describe the fault" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} />
          <select value={form.urgency} onChange={(event) => setForm({ ...form, urgency: event.target.value })}>
            {URGENCIES.map((urgency) => (
              <option key={urgency}>{urgency}</option>
            ))}
          </select>
          <button type="submit">Report</button>
        </form>
      )}

      {isAdmin && (
        <div className="card">
          <h3>Activity log</h3>
          <ul className="notes">
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
