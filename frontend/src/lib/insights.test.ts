import { describe, expect, it } from 'vitest';
import type { Equipment, MaintenanceRequest, Session } from '../types';
import {
  addDays,
  dateOf,
  formatDay,
  nextSession,
  requestCounts,
  sessionsOn,
  summarizeEquipment,
  timeOf,
  todayIso,
  weekDays,
  weekStart,
} from './insights';

function equipment(overrides: Partial<Equipment> = {}): Equipment {
  return {
    id: 'TM-01',
    name: 'Treadmill 01',
    type: 'TREADMILL',
    location: 'Cardio Zone',
    status: 'OPERATIONAL',
    active: true,
    totalUsageHours: 0,
    hoursSinceMaintenance: 0,
    maintenanceThresholdHours: 100,
    needsMaintenance: false,
    ...overrides,
  };
}

function session(id: string, start: string, overrides: Partial<Session> = {}): Session {
  return {
    id,
    title: `Class ${id}`,
    instructorId: 'I-1',
    instructorName: 'Nimali',
    studio: 'Studio A',
    start,
    end: start,
    capacity: 10,
    booked: 0,
    availableSpots: 10,
    equipmentIds: [],
    ...overrides,
  };
}

function request(status: MaintenanceRequest['status']): MaintenanceRequest {
  return {
    id: `MR-${status}`,
    equipmentId: 'TM-01',
    description: 'Belt',
    urgency: 'LOW',
    status,
    reportedBy: 'Nimali',
    assignedTo: null,
    progressNotes: [],
  };
}

describe('summarizeEquipment', () => {
  it('should count nothing when there is no equipment', () => {
    expect(summarizeEquipment([])).toEqual({
      total: 0,
      operational: 0,
      faulty: 0,
      underMaintenance: 0,
      deactivated: 0,
      maintenanceDue: 0,
    });
  });

  it('should count one operational item', () => {
    expect(summarizeEquipment([equipment()])).toMatchObject({ total: 1, operational: 1 });
  });

  it('should count every status separately when there are many items', () => {
    const summary = summarizeEquipment([
      equipment({ id: 'A' }),
      equipment({ id: 'B', status: 'FAULTY' }),
      equipment({ id: 'C', status: 'UNDER_MAINTENANCE' }),
      equipment({ id: 'D', status: 'FAULTY' }),
    ]);

    expect(summary).toMatchObject({ total: 4, operational: 1, faulty: 2, underMaintenance: 1 });
  });

  it('should keep deactivated equipment out of the status counts', () => {
    const summary = summarizeEquipment([equipment({ active: false, status: 'FAULTY' })]);

    expect(summary).toMatchObject({ total: 1, deactivated: 1, faulty: 0, operational: 0 });
  });

  it('should count maintenance due only for active equipment', () => {
    const summary = summarizeEquipment([
      equipment({ id: 'A', needsMaintenance: true }),
      equipment({ id: 'B', needsMaintenance: true, active: false }),
    ]);

    expect(summary.maintenanceDue).toBe(1);
  });
});

describe('dates', () => {
  it('should move across a month boundary and into a leap day', () => {
    expect(addDays('2026-10-31', 1)).toBe('2026-11-01');
    expect(addDays('2028-02-28', 1)).toBe('2028-02-29');
    expect(addDays('2026-01-01', -1)).toBe('2025-12-31');
  });

  it('should find the Monday of the week for any day of it', () => {
    expect(weekStart('2026-10-05')).toBe('2026-10-05'); // Monday
    expect(weekStart('2026-10-07')).toBe('2026-10-05'); // Wednesday
    expect(weekStart('2026-10-11')).toBe('2026-10-05'); // Sunday
    expect(weekStart('2026-10-12')).toBe('2026-10-12'); // next Monday
  });

  it('should list the seven days of a week from Monday to Sunday', () => {
    const days = weekDays('2026-10-08');

    expect(days).toHaveLength(7);
    expect(days[0]).toBe('2026-10-05');
    expect(days[6]).toBe('2026-10-11');
  });

  it('should read the date and the time out of a session timestamp', () => {
    expect(dateOf('2026-10-05T09:00')).toBe('2026-10-05');
    expect(timeOf('2026-10-05T09:30')).toBe('09:30');
  });

  it('should format a day as short weekday, day and month', () => {
    expect(formatDay('2026-10-05')).toBe('Mon 05 Oct');
    expect(formatDay('2026-12-25')).toBe('Fri 25 Dec');
  });

  it('should write todays local date with leading zeros', () => {
    expect(todayIso(new Date(2026, 0, 5, 23, 59))).toBe('2026-01-05');
  });
});

describe('sessions', () => {
  const sessions = [
    session('late', '2026-10-05T18:00'),
    session('early', '2026-10-05T09:00'),
    session('tuesday', '2026-10-06T09:00'),
  ];

  it('should give the sessions of one day in start order', () => {
    expect(sessionsOn(sessions, '2026-10-05').map((s) => s.id)).toEqual(['early', 'late']);
  });

  it('should give nothing for a day without sessions', () => {
    expect(sessionsOn(sessions, '2026-10-07')).toEqual([]);
  });

  it('should find the next session that has not started yet', () => {
    expect(nextSession(sessions, '2026-10-05T10:00')?.id).toBe('late');
    expect(nextSession(sessions, '2026-10-05T09:00')?.id).toBe('early');
  });

  it('should find no next session when all are in the past', () => {
    expect(nextSession(sessions, '2026-10-07T00:00')).toBeUndefined();
    expect(nextSession([], '2026-10-05T00:00')).toBeUndefined();
  });
});

describe('requestCounts', () => {
  it('should count requests by status and treat pending plus assigned as open', () => {
    const counts = requestCounts([request('PENDING'), request('PENDING'), request('ASSIGNED'), request('COMPLETED')]);

    expect(counts).toEqual({ pending: 2, assigned: 1, completed: 1, open: 3 });
  });

  it('should count zero for no requests', () => {
    expect(requestCounts([])).toEqual({ pending: 0, assigned: 0, completed: 0, open: 0 });
  });
});
