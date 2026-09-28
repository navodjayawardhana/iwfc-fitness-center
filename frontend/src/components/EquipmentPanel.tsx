import { useState, type FormEvent } from 'react';
import { call } from '../api';
import { useAction, useLoad } from '../hooks';
import type { Equipment, Report, User } from '../types';
import { btn, btnDanger, card, chip, field, muted } from '../ui';

const TYPES = ['TREADMILL', 'SPIN_BIKE', 'ROWING_MACHINE', 'HEART_RATE_MONITOR'];

interface Props {
  user: User;
  report: Report;
}

function UsageBar({ item }: { item: Equipment }) {
  const share = Math.min(100, (item.hoursSinceMaintenance / item.maintenanceThresholdHours) * 100);
  return (
    <div
      className="mb-1 h-2 w-36 overflow-hidden rounded-full bg-slate-200 dark:bg-slate-700"
      title={`${item.hoursSinceMaintenance} of ${item.maintenanceThresholdHours} hours`}
    >
      <div className={`h-full ${item.needsMaintenance ? 'bg-red-600' : 'bg-teal-600'}`} style={{ width: `${share}%` }} />
    </div>
  );
}

function statusChip(item: Equipment) {
  if (!item.active) return chip.neutral;
  if (item.status === 'OPERATIONAL') return chip.good;
  if (item.status === 'FAULTY') return chip.bad;
  return chip.warn;
}

export default function EquipmentPanel({ user, report }: Props) {
  const { data: items, reload } = useLoad(() => call<Equipment[]>('GET', '/equipment'), [user.id], report);
  const run = useAction(report, reload);
  const [form, setForm] = useState({ type: 'TREADMILL', id: '', name: '', location: 'Cardio Zone' });
  const [hours, setHours] = useState<Record<string, string>>({});

  const isAdmin = user.role === 'Administrator';
  const isInstructor = user.role === 'Instructor';

  const add = (event: FormEvent) => {
    event.preventDefault();
    void run(() => call('POST', '/equipment', form), `Added ${form.id}`);
  };

  return (
    <section>
      <h2 className="mt-2 mb-3 text-xl font-semibold">Equipment</h2>
      <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-900">
        <table className="w-full border-collapse text-left text-sm">
          <thead>
            <tr className={`text-xs tracking-wide uppercase ${muted}`}>
              <th className="p-3">ID</th>
              <th className="p-3">Name</th>
              <th className="hidden p-3 sm:table-cell">Location</th>
              <th className="p-3">Status</th>
              <th className="p-3">Usage since maintenance</th>
              {(isAdmin || isInstructor) && <th className="p-3">Actions</th>}
            </tr>
          </thead>
          <tbody>
            {(items ?? []).map((item) => (
              <tr key={item.id} className={`border-t border-slate-200 dark:border-slate-700 ${item.active ? '' : 'opacity-55'}`}>
                <td className="p-3">{item.id}</td>
                <td className="p-3">{item.name}</td>
                <td className="hidden p-3 sm:table-cell">{item.location}</td>
                <td className="p-3">
                  <span className={statusChip(item)}>{item.active ? item.status.replace('_', ' ') : 'DEACTIVATED'}</span>
                  {item.needsMaintenance && <span className={chip.bad}>Maintenance due</span>}
                </td>
                <td className="p-3">
                  <UsageBar item={item} />
                  <small className={muted}>
                    {item.hoursSinceMaintenance.toFixed(1)} / {item.maintenanceThresholdHours} h
                  </small>
                </td>
                {(isAdmin || isInstructor) && (
                  <td className="p-3">
                    <div className="flex flex-wrap items-center gap-1.5">
                      {isInstructor && (
                        <>
                          <input
                            className={`${field} w-24`}
                            type="number"
                            min="0.5"
                            step="0.5"
                            placeholder="hours"
                            value={hours[item.id] ?? ''}
                            onChange={(event) => setHours({ ...hours, [item.id]: event.target.value })}
                          />
                          <button
                            className={btn}
                            onClick={() =>
                              void run(
                                () => call('POST', `/equipment/${item.id}/usage`, { hours: Number(hours[item.id]) }),
                                `Logged usage on ${item.id}`,
                              )
                            }
                          >
                            Log usage
                          </button>
                        </>
                      )}
                      {isAdmin && item.active && (
                        <button
                          className={btnDanger}
                          onClick={() => void run(() => call('POST', `/equipment/${item.id}/deactivate`), `Deactivated ${item.id}`)}
                        >
                          Deactivate
                        </button>
                      )}
                    </div>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {isAdmin ? (
        <form className={`${card} mt-4 flex flex-wrap items-center gap-2`} onSubmit={add}>
          <h3 className="w-full font-semibold">Add equipment</h3>
          <select className={field} value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}>
            {TYPES.map((type) => (
              <option key={type}>{type}</option>
            ))}
          </select>
          <input className={field} placeholder="ID (e.g. TM-03)" value={form.id} onChange={(event) => setForm({ ...form, id: event.target.value })} />
          <input className={field} placeholder="Name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          <input className={field} placeholder="Location" value={form.location} onChange={(event) => setForm({ ...form, location: event.target.value })} />
          <button className={btn} type="submit">
            Add
          </button>
        </form>
      ) : (
        <p className={`mt-4 ${muted}`}>Only administrators can add or deactivate equipment. Try switching user.</p>
      )}
    </section>
  );
}
