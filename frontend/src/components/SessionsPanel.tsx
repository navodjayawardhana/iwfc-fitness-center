import { useState, type FormEvent } from 'react';
import { call } from '../api';
import { useAction, useLoad } from '../hooks';
import type { Report, Session, User } from '../types';
import { btn, btnDanger, btnGhost, card, chip, field, muted } from '../ui';

interface Props {
  user: User;
  report: Report;
}

function nextMonday(): string {
  const date = new Date();
  date.setDate(date.getDate() + ((8 - date.getDay()) % 7 || 7));
  return date.toISOString().slice(0, 10);
}

function timePart(iso: string): string {
  return (iso.split('T')[1] ?? '').slice(0, 5);
}

function formatWhen(iso: string): string {
  return `${iso.split('T')[0] ?? ''} ${timePart(iso)}`;
}

export default function SessionsPanel({ user, report }: Props) {
  const { data: sessions, reload } = useLoad(() => call<Session[]>('GET', '/sessions?all=true', user.id), [user.id], report);
  const run = useAction(report, reload);
  const [form, setForm] = useState({
    id: '',
    title: '',
    studio: 'Studio B',
    date: nextMonday(),
    start: '14:00',
    end: '15:00',
    capacity: '10',
    equipment: '',
    weeks: '1',
  });

  const isInstructor = user.role === 'Instructor';
  const isMember = user.role === 'Member';

  const schedule = (event: FormEvent) => {
    event.preventDefault();
    const body = {
      id: form.id,
      title: form.title,
      studio: form.studio,
      date: form.date,
      start: form.start,
      end: form.end,
      capacity: Number(form.capacity),
      weeks: Number(form.weeks),
      equipmentIds: form.equipment.split(',').map((id) => id.trim()).filter(Boolean),
    };
    void run(() => call('POST', '/sessions', user.id, body), `Scheduled ${form.title}`);
  };

  const set = (key: keyof typeof form) => (event: { target: { value: string } }) => setForm({ ...form, [key]: event.target.value });

  return (
    <section>
      <h2 className="mt-2 mb-3 text-xl font-semibold">Sessions</h2>
      <div className="mb-4 grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-3">
        {(sessions ?? []).map((session) => (
          <article className={card} key={session.id}>
            <h3 className="font-semibold">{session.title}</h3>
            <p className={`text-sm ${muted}`}>
              {session.id} · {session.studio} · {session.instructorName}
            </p>
            <p className="my-1">
              {formatWhen(session.start)} – {timePart(session.end)}
            </p>
            {session.equipmentIds.length > 0 && <p className={`text-sm ${muted}`}>Equipment: {session.equipmentIds.join(', ')}</p>}
            <p className="my-1">
              <span className={session.availableSpots === 0 ? chip.bad : chip.good}>
                {session.availableSpots === 0 ? 'Full' : `${session.availableSpots} spots left`}
              </span>
              <small className={muted}>
                {session.booked}/{session.capacity} booked
              </small>
            </p>
            <div className="mt-2 flex flex-wrap items-center gap-1.5">
              {isMember && (
                <>
                  <button className={btn} onClick={() => void run(() => call('POST', `/sessions/${session.id}/bookings`, user.id), `Booked ${session.title}`)}>
                    Book
                  </button>
                  <button className={btnGhost} onClick={() => void run(() => call('DELETE', `/sessions/${session.id}/bookings`, user.id), 'Booking cancelled')}>
                    Cancel booking
                  </button>
                </>
              )}
              {isInstructor && (
                <>
                  <button className={btn} onClick={() => void run(() => call('POST', `/sessions/${session.id}/complete`, user.id), 'Usage logged for the session equipment')}>
                    Complete
                  </button>
                  <button className={btnDanger} onClick={() => void run(() => call('DELETE', `/sessions/${session.id}`, user.id), `Cancelled ${session.title}`)}>
                    Cancel session
                  </button>
                </>
              )}
            </div>
          </article>
        ))}
      </div>

      {isInstructor ? (
        <form className={`${card} flex flex-wrap items-center gap-2`} onSubmit={schedule}>
          <h3 className="w-full font-semibold">Schedule a session</h3>
          <input className={field} placeholder="ID (e.g. S-9)" value={form.id} onChange={set('id')} />
          <input className={field} placeholder="Title" value={form.title} onChange={set('title')} />
          <input className={field} placeholder="Studio" value={form.studio} onChange={set('studio')} />
          <input className={field} type="date" value={form.date} onChange={set('date')} />
          <input className={field} type="time" value={form.start} onChange={set('start')} />
          <input className={field} type="time" value={form.end} onChange={set('end')} />
          <input className={`${field} w-24`} type="number" min="1" title="Capacity" value={form.capacity} onChange={set('capacity')} />
          <input className={field} placeholder="Equipment IDs, comma separated" value={form.equipment} onChange={set('equipment')} />
          <label className={`flex items-center gap-1.5 text-sm ${muted}`}>
            Repeat weekly for
            <input className={`${field} w-16`} type="number" min="1" max="12" value={form.weeks} onChange={set('weeks')} />
            weeks
          </label>
          <button className={btn} type="submit">
            Schedule
          </button>
        </form>
      ) : (
        <p className={muted}>{isMember ? 'Book a session above.' : 'Only instructors schedule sessions.'}</p>
      )}
    </section>
  );
}
