import { useState } from 'react';
import { addDays, formatDay, nowIso, sessionsOn, timeOf, todayIso, weekDays, weekStart } from '../lib/insights';
import type { Session } from '../types';
import { btnGhost, card, chip, muted } from '../ui';

interface Props {
  sessions: Session[];
}

/** Monday to Sunday, one column per day (stacked on a phone). Starts on the week of the next session. */
export default function TimetableView({ sessions }: Props) {
  const today = todayIso();
  const firstWeek = weekStart(
    [...sessions].filter((session) => session.start >= nowIso()).sort((a, b) => a.start.localeCompare(b.start))[0]?.start.slice(0, 10) ?? today,
  );
  const [monday, setMonday] = useState(firstWeek);
  const days = weekDays(monday);

  return (
    <div>
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <button className={btnGhost} onClick={() => setMonday(addDays(monday, -7))}>
          ← Previous week
        </button>
        <button className={btnGhost} onClick={() => setMonday(weekStart(today))}>
          This week
        </button>
        <button className={btnGhost} onClick={() => setMonday(addDays(monday, 7))}>
          Next week →
        </button>
        <span className={`text-sm ${muted}`}>
          {formatDay(days[0] ?? monday)} – {formatDay(days[6] ?? monday)}
        </span>
      </div>

      <div className="grid gap-2 md:grid-cols-7">
        {days.map((day) => {
          const daySessions = sessionsOn(sessions, day);
          return (
            <section key={day} className={`${card} min-h-24 p-3`} aria-label={formatDay(day)}>
              <h3 className={`mb-2 text-sm font-semibold ${day === today ? 'text-teal-700 dark:text-teal-300' : ''}`}>
                {formatDay(day)}
                {day === today && <span className="ml-1 text-xs">(today)</span>}
              </h3>
              {daySessions.length === 0 && <p className={`text-xs ${muted}`}>No sessions</p>}
              <ul className="flex flex-col gap-2">
                {daySessions.map((session) => (
                  <li key={session.id} className="rounded-lg bg-slate-100 p-2 text-xs dark:bg-slate-800">
                    <p className="font-semibold">
                      {timeOf(session.start)}–{timeOf(session.end)} {session.title}
                    </p>
                    <p className={muted}>{session.studio}</p>
                    <p>
                      <span className={session.availableSpots === 0 ? chip.bad : chip.good}>
                        {session.availableSpots === 0 ? 'Full' : `${session.availableSpots} left`}
                      </span>
                    </p>
                  </li>
                ))}
              </ul>
            </section>
          );
        })}
      </div>
    </div>
  );
}
