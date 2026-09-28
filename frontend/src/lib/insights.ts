// Pure helpers behind the dashboard and the weekly timetable. No React and no network, so they are easy to test.
// Dates travel as ISO text ("2026-10-05", "2026-10-05T09:00") and are handled in UTC so daylight saving never shifts a day.

import type { Equipment, MaintenanceRequest, Session } from '../types';

export interface EquipmentSummary {
  total: number;
  operational: number;
  faulty: number;
  underMaintenance: number;
  deactivated: number;
  maintenanceDue: number;
}

/** Deactivated equipment is counted on its own and left out of the status counts. */
export function summarizeEquipment(items: Equipment[]): EquipmentSummary {
  const summary: EquipmentSummary = {
    total: items.length,
    operational: 0,
    faulty: 0,
    underMaintenance: 0,
    deactivated: 0,
    maintenanceDue: 0,
  };
  for (const item of items) {
    if (!item.active) {
      summary.deactivated += 1;
      continue;
    }
    if (item.status === 'OPERATIONAL') summary.operational += 1;
    else if (item.status === 'FAULTY') summary.faulty += 1;
    else summary.underMaintenance += 1;
    if (item.needsMaintenance) summary.maintenanceDue += 1;
  }
  return summary;
}

export interface RequestCounts {
  pending: number;
  assigned: number;
  completed: number;
  open: number;
}

export function requestCounts(requests: MaintenanceRequest[]): RequestCounts {
  const counts = { pending: 0, assigned: 0, completed: 0, open: 0 };
  for (const request of requests) {
    if (request.status === 'PENDING') counts.pending += 1;
    else if (request.status === 'ASSIGNED') counts.assigned += 1;
    else counts.completed += 1;
  }
  counts.open = counts.pending + counts.assigned;
  return counts;
}

const DAY_MS = 24 * 60 * 60 * 1000;
const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

function parse(isoDate: string): Date {
  const [year = 1970, month = 1, day = 1] = isoDate.slice(0, 10).split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day));
}

function toIso(date: Date): string {
  return date.toISOString().slice(0, 10);
}

export function addDays(isoDate: string, days: number): string {
  return toIso(new Date(parse(isoDate).getTime() + days * DAY_MS));
}

/** The Monday of the week that contains the given day. */
export function weekStart(isoDate: string): string {
  const sinceMonday = (parse(isoDate).getUTCDay() + 6) % 7;
  return addDays(isoDate, -sinceMonday);
}

/** Monday to Sunday of the week that contains the given day. */
export function weekDays(isoDate: string): string[] {
  const monday = weekStart(isoDate);
  return Array.from({ length: 7 }, (_, offset) => addDays(monday, offset));
}

export function dateOf(timestamp: string): string {
  return timestamp.slice(0, 10);
}

export function timeOf(timestamp: string): string {
  return timestamp.slice(11, 16);
}

/** "Mon 05 Oct" */
export function formatDay(isoDate: string): string {
  const date = parse(isoDate);
  return `${WEEKDAYS[date.getUTCDay()]} ${String(date.getUTCDate()).padStart(2, '0')} ${MONTHS[date.getUTCMonth()]}`;
}

/** The local calendar date of a moment, as ISO text. */
export function todayIso(now: Date = new Date()): string {
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

/** The local date and time of a moment, like a session timestamp ("2026-10-05T09:00"). */
export function nowIso(now: Date = new Date()): string {
  return `${todayIso(now)}T${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
}

/** The sessions on one day, earliest first. */
export function sessionsOn(sessions: Session[], isoDate: string): Session[] {
  return sessions.filter((session) => dateOf(session.start) === isoDate).sort((a, b) => a.start.localeCompare(b.start));
}

/** The earliest session that has not started yet. */
export function nextSession(sessions: Session[], now: string): Session | undefined {
  return [...sessions].filter((session) => session.start >= now).sort((a, b) => a.start.localeCompare(b.start))[0];
}
