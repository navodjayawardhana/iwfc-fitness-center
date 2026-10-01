import { useState, type FormEvent } from 'react';
import { call } from '../api';
import Drawer from './Drawer';
import { useAction, useLoad } from '../hooks';
import TimetableView from './TimetableView';
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
  const { data: sessions, reload } = useLoad(() => call<Session[]>('GET', '/sessions?all=true'), [user.id], report);
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

  const [view, setView] = useState<'list' | 'week'>('list');
  const [scheduling, setScheduling] = useState(false);

  const isInstructor = user.role === 'Instructor';
  const isMember = user.role === 'Member';

  const schedule = async (event: FormEvent) => {
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
    if (await run(() => call('POST', '/sessions', body), `Scheduled ${form.title}`)) {
      setScheduling(false);
    }
  };

  const set = (key: keyof typeof form) => (event: { target: { value: string } }) => setForm({ ...form, [key]: event.target.value });

  return (
    <section>
      <div className="mt-2 mb-3 flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-xl font-semibold">Sessions</h2>
        <div className="flex flex-wrap gap-1" role="group" aria-label="Session view">
          <button className={view === 'list' ? btn : btnGhost} aria-pressed={view === 'list'} onClick={() => setView('list')}>
            List
          </button>
          <button className={view === 'week' ? btn : btnGhost} aria-pressed={view === 'week'} onClick={() => setView('week')}>
            Week
          </button>
          {isInstructor && (
            <button className={btn} onClick={() => setScheduling(true)}>
              + Schedule a session
            </button>
          )}
        </div>
      </div>
      {view === 'week' && <TimetableView sessions={sessions ?? []} />}
      <div className={view === 'week' ? 'hidden' : 'mb-4 grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-3'}>
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
                  <button className={btn} onClick={() => void run(() => call('POST', `/sessions/${session.id}/bookings`), `Booked ${session.title}`)}>
                    Book
                  </button>
                  <button className={btnGhost} onClick={() => void run(() => call('DELETE', `/sessions/${session.id}/bookings`), 'Booking cancelled')}>
                    Cancel booking
                  </button>
                </>
              )}
              {isInstructor && (
                <>
                  <button className={btn} onClick={() => void run(() => call('POST', `/sessions/${session.id}/complete`), 'Usage logged for the session equipment')}>
                    Complete
                  </button>
                  <button className={btnDanger} onClick={() => void run(() => call('DELETE', `/sessions/${session.id}`), `Cancelled ${session.title}`)}>
                    Cancel session
                  </button>
                </>
              )}
            </div>
          </article>
        ))}
      </div>

      {!isInstructor && <p className={muted}>{isMember ? 'Book a session above.' : 'Only instructors schedule sessions.'}</p>}

      <Drawer open={scheduling} title="Schedule a session" onClose={() => setScheduling(false)}>
        <form className="flex flex-col gap-3" onSubmit={(event) => void schedule(event)}>
          <label className="flex flex-col gap-1 text-sm">
            ID
            <input className={field} placeholder="e.g. S-9" value={form.id} onChange={set('id')} required />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            Title
            <input className={field} value={form.title} onChange={set('title')} required />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            Studio
            <input className={field} value={form.studio} onChange={set('studio')} required />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            Date
            <input className={field} type="date" value={form.date} onChange={set('date')} required />
          </label>
          <div className="flex gap-3">
            <label className="flex flex-1 flex-col gap-1 text-sm">
              Starts
              <input className={field} type="time" value={form.start} onChange={set('start')} required />
            </label>
            <label className="flex flex-1 flex-col gap-1 text-sm">
              Ends
              <input className={field} type="time" value={form.end} onChange={set('end')} required />
            </label>
          </div>
          <label className="flex flex-col gap-1 text-sm">
            Capacity
            <input className={field} type="number" min="1" value={form.capacity} onChange={set('capacity')} required />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            Equipment IDs (comma separated, optional)
            <input className={field} placeholder="e.g. TM-01, SB-04" value={form.equipment} onChange={set('equipment')} />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            Repeat weekly for (weeks)
            <input className={field} type="number" min="1" max="12" value={form.weeks} onChange={set('weeks')} />
          </label>
          <button className={`${btn} mt-2`} type="submit">
            Schedule
          </button>
        </form>
      </Drawer>
    </section>
  );
}
