import { call } from '../api';
import { useLoad } from '../hooks';
import { formatDay, nextSession, nowIso, requestCounts, sessionsOn, summarizeEquipment, timeOf, todayIso } from '../lib/insights';
import type { Equipment, MaintenanceRequest, Report, Session, User } from '../types';
import { btn, card, chip, muted } from '../ui';

export type DashboardTarget = 'equipment' | 'sessions' | 'maintenance' | 'notifications';

interface Props {
  user: User;
  report: Report;
  goTo: (tab: DashboardTarget) => void;
}

function Stat({ label, value, tone = 'neutral', hint }: { label: string; value: number | string; tone?: keyof typeof chip; hint?: string }) {
  return (
    <div className={`${card} flex flex-col gap-1`}>
      <p className={`text-xs tracking-wide uppercase ${muted}`}>{label}</p>
      <p className="text-3xl font-bold">{value}</p>
      <p className="text-sm">
        <span className={chip[tone]}>{hint ?? ' '}</span>
      </p>
    </div>
  );
}

/** A quick picture of the centre, built only from the endpoints the other tabs already use. */
export default function DashboardPanel({ user, report, goTo }: Props) {
  const isAdmin = user.role === 'Administrator';
  const isInstructor = user.role === 'Instructor';

  const equipment = useLoad(() => call<Equipment[]>('GET', '/equipment'), [user.id], report);
  const sessions = useLoad(() => call<Session[]>('GET', '/sessions?all=true'), [user.id], report);
  const inbox = useLoad(() => call<string[]>('GET', '/notifications'), [user.id], report);
  // Members cannot read maintenance requests, so they are not asked for them.
  const requests = useLoad(
    () => (isAdmin || isInstructor ? call<MaintenanceRequest[]>('GET', isAdmin ? '/maintenance' : '/maintenance?mine=true') : Promise.resolve<MaintenanceRequest[]>([])),
    [user.id],
    report,
  );

  const summary = summarizeEquipment(equipment.data ?? []);
  const today = todayIso();
  const todays = sessionsOn(sessions.data ?? [], today);
  const upcoming = nextSession(sessions.data ?? [], nowIso());
  const counts = requestCounts(requests.data ?? []);
  const needsAttention = summary.faulty + summary.underMaintenance;

  return (
    <section>
      <h2 className="mt-2 mb-1 text-xl font-semibold">Welcome, {user.name}</h2>
      <p className={`mb-4 ${muted}`}>{formatDay(today)} · signed in as {user.role}</p>

      <div className="grid grid-cols-[repeat(auto-fill,minmax(210px,1fr))] gap-3">
        <Stat label="Equipment" value={summary.total} tone="neutral" hint={`${summary.operational} operational`} />
        <Stat label="Needs attention" value={needsAttention} tone={needsAttention > 0 ? 'bad' : 'good'} hint={needsAttention > 0 ? 'faulty or in repair' : 'all clear'} />
        <Stat label="Maintenance due" value={summary.maintenanceDue} tone={summary.maintenanceDue > 0 ? 'warn' : 'good'} hint={summary.maintenanceDue > 0 ? 'usage limit reached' : 'none due'} />
        <Stat label="Sessions today" value={todays.length} tone="info" hint={todays.length === 0 ? 'nothing today' : 'on the timetable'} />
        {(isAdmin || isInstructor) && (
          <Stat label={isAdmin ? 'Open requests' : 'My open requests'} value={counts.open} tone={counts.open > 0 ? 'warn' : 'good'} hint={`${counts.pending} pending · ${counts.assigned} assigned`} />
        )}
        <Stat label="Notifications" value={(inbox.data ?? []).length} tone="info" hint="in your inbox" />
      </div>

      <div className="mt-4 grid gap-3 md:grid-cols-2">
        <div className={card}>
          <h3 className="mb-2 font-semibold">Next session</h3>
          {upcoming ? (
            <>
              <p className="text-lg font-semibold">{upcoming.title}</p>
              <p className={muted}>
                {formatDay(upcoming.start.slice(0, 10))} · {timeOf(upcoming.start)}–{timeOf(upcoming.end)} · {upcoming.studio} · {upcoming.instructorName}
              </p>
              <p className="mt-1">
                <span className={upcoming.availableSpots === 0 ? chip.bad : chip.good}>
                  {upcoming.availableSpots === 0 ? 'Full' : `${upcoming.availableSpots} spots left`}
                </span>
              </p>
            </>
          ) : (
            <p className={muted}>No upcoming sessions.</p>
          )}
        </div>

        <div className={card}>
          <h3 className="mb-2 font-semibold">Quick actions</h3>
          <div className="flex flex-wrap gap-2">
            <button className={btn} onClick={() => goTo('sessions')}>
              {user.role === 'Member' ? 'Book a session' : 'Open sessions'}
            </button>
            <button className={btn} onClick={() => goTo('equipment')}>
              View equipment
            </button>
            {(isAdmin || isInstructor) && (
              <button className={btn} onClick={() => goTo('maintenance')}>
                {isInstructor ? 'Report a fault' : 'Review requests'}
              </button>
            )}
            <button className={btn} onClick={() => goTo('notifications')}>
              Notifications
            </button>
          </div>
        </div>
      </div>
    </section>
  );
}
