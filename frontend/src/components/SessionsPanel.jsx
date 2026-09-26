import { useState } from 'react';
import { call } from '../api.js';
import { useAction, useLoad } from '../hooks.js';

function nextMonday() {
  const date = new Date();
  date.setDate(date.getDate() + ((8 - date.getDay()) % 7 || 7));
  return date.toISOString().slice(0, 10);
}

function formatWhen(iso) {
  const [date, time] = iso.split('T');
  return `${date} ${time.slice(0, 5)}`;
}

export default function SessionsPanel({ user, report }) {
  const { data: sessions, reload } = useLoad(() => call('GET', '/sessions?all=true', user.id), [user.id], report);
  const run = useAction(report, reload);
  const [form, setForm] = useState({
    id: '',
    title: '',
    studio: 'Studio B',
    date: nextMonday(),
    start: '14:00',
    end: '15:00',
    capacity: 10,
    equipment: '',
    weeks: 1,
  });

  const isInstructor = user.role === 'Instructor';
  const isMember = user.role === 'Member';

  const schedule = (event) => {
    event.preventDefault();
    const body = {
      ...form,
      capacity: Number(form.capacity),
      weeks: Number(form.weeks),
      equipmentIds: form.equipment.split(',').map((id) => id.trim()).filter(Boolean),
    };
    run(() => call('POST', '/sessions', user.id, body), `Scheduled ${form.title}`);
  };

  return (
    <section>
      <h2>Sessions</h2>
      <div className="grid">
        {(sessions ?? []).map((session) => (
          <article className="card" key={session.id}>
            <h3>{session.title}</h3>
            <p className="meta">
              {session.id} · {session.studio} · {session.instructorName}
            </p>
            <p>
              {formatWhen(session.start)} – {session.end.split('T')[1].slice(0, 5)}
            </p>
            {session.equipmentIds.length > 0 && <p className="meta">Equipment: {session.equipmentIds.join(', ')}</p>}
            <p>
              <span className={session.availableSpots === 0 ? 'chip faulty' : 'chip operational'}>
                {session.availableSpots === 0 ? 'Full' : `${session.availableSpots} spots left`}
              </span>{' '}
              <small>
                {session.booked}/{session.capacity} booked
              </small>
            </p>
            <div className="actions">
              {isMember && (
                <>
                  <button onClick={() => run(() => call('POST', `/sessions/${session.id}/bookings`, user.id), `Booked ${session.title}`)}>
                    Book
                  </button>
                  <button className="ghost" onClick={() => run(() => call('DELETE', `/sessions/${session.id}/bookings`, user.id), 'Booking cancelled')}>
                    Cancel booking
                  </button>
                </>
              )}
              {isInstructor && (
                <>
                  <button onClick={() => run(() => call('POST', `/sessions/${session.id}/complete`, user.id), 'Usage logged for the session equipment')}>
                    Complete
                  </button>
                  <button className="danger" onClick={() => run(() => call('DELETE', `/sessions/${session.id}`, user.id), `Cancelled ${session.title}`)}>
                    Cancel session
                  </button>
                </>
              )}
            </div>
          </article>
        ))}
      </div>

      {isInstructor ? (
        <form className="card form" onSubmit={schedule}>
          <h3>Schedule a session</h3>
          <input placeholder="ID (e.g. S-9)" value={form.id} onChange={(event) => setForm({ ...form, id: event.target.value })} />
          <input placeholder="Title" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} />
          <input placeholder="Studio" value={form.studio} onChange={(event) => setForm({ ...form, studio: event.target.value })} />
          <input type="date" value={form.date} onChange={(event) => setForm({ ...form, date: event.target.value })} />
          <input type="time" value={form.start} onChange={(event) => setForm({ ...form, start: event.target.value })} />
          <input type="time" value={form.end} onChange={(event) => setForm({ ...form, end: event.target.value })} />
          <input type="number" min="1" title="Capacity" value={form.capacity} onChange={(event) => setForm({ ...form, capacity: event.target.value })} />
          <input placeholder="Equipment IDs, comma separated" value={form.equipment} onChange={(event) => setForm({ ...form, equipment: event.target.value })} />
          <label className="inline">
            Repeat weekly for
            <input type="number" min="1" max="12" value={form.weeks} onChange={(event) => setForm({ ...form, weeks: event.target.value })} />
            weeks
          </label>
          <button type="submit">Schedule</button>
        </form>
      ) : (
        <p className="hint">{isMember ? 'Book a session above.' : 'Only instructors schedule sessions.'}</p>
      )}
    </section>
  );
}
