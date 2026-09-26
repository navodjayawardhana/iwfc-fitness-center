import { useState } from 'react';
import { call } from '../api.js';
import { useAction, useLoad } from '../hooks.js';

const TYPES = ['TREADMILL', 'SPIN_BIKE', 'ROWING_MACHINE', 'HEART_RATE_MONITOR'];

function UsageBar({ item }) {
  const share = Math.min(100, (item.hoursSinceMaintenance / item.maintenanceThresholdHours) * 100);
  return (
    <div className="usage" title={`${item.hoursSinceMaintenance} of ${item.maintenanceThresholdHours} hours`}>
      <div className={item.needsMaintenance ? 'usage-fill due' : 'usage-fill'} style={{ width: `${share}%` }} />
    </div>
  );
}

export default function EquipmentPanel({ user, report }) {
  const { data: items, reload } = useLoad(() => call('GET', '/equipment', user.id), [user.id], report);
  const run = useAction(report, reload);
  const [form, setForm] = useState({ type: 'TREADMILL', id: '', name: '', location: 'Cardio Zone' });
  const [hours, setHours] = useState({});

  const isAdmin = user.role === 'Administrator';
  const isInstructor = user.role === 'Instructor';

  const add = (event) => {
    event.preventDefault();
    run(() => call('POST', '/equipment', user.id, form), `Added ${form.id}`);
  };

  return (
    <section>
      <h2>Equipment</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Location</th>
              <th>Status</th>
              <th>Usage since maintenance</th>
              {(isAdmin || isInstructor) && <th>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {(items ?? []).map((item) => (
              <tr key={item.id} className={item.active ? '' : 'inactive'}>
                <td>{item.id}</td>
                <td>{item.name}</td>
                <td>{item.location}</td>
                <td>
                  <span className={`chip ${item.active ? item.status.toLowerCase() : 'deactivated'}`}>
                    {item.active ? item.status.replace('_', ' ') : 'DEACTIVATED'}
                  </span>
                  {item.needsMaintenance && <span className="chip due">Maintenance due</span>}
                </td>
                <td>
                  <UsageBar item={item} />
                  <small>
                    {item.hoursSinceMaintenance.toFixed(1)} / {item.maintenanceThresholdHours} h
                  </small>
                </td>
                {(isAdmin || isInstructor) && (
                  <td className="actions">
                    {isInstructor && (
                      <>
                        <input
                          type="number"
                          min="0.5"
                          step="0.5"
                          placeholder="hours"
                          value={hours[item.id] ?? ''}
                          onChange={(event) => setHours({ ...hours, [item.id]: event.target.value })}
                        />
                        <button
                          onClick={() =>
                            run(
                              () => call('POST', `/equipment/${item.id}/usage`, user.id, { hours: Number(hours[item.id]) }),
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
                        className="danger"
                        onClick={() => run(() => call('POST', `/equipment/${item.id}/deactivate`, user.id), `Deactivated ${item.id}`)}
                      >
                        Deactivate
                      </button>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {isAdmin && (
        <form className="card form" onSubmit={add}>
          <h3>Add equipment</h3>
          <select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}>
            {TYPES.map((type) => (
              <option key={type}>{type}</option>
            ))}
          </select>
          <input placeholder="ID (e.g. TM-03)" value={form.id} onChange={(event) => setForm({ ...form, id: event.target.value })} />
          <input placeholder="Name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          <input placeholder="Location" value={form.location} onChange={(event) => setForm({ ...form, location: event.target.value })} />
          <button type="submit">Add</button>
        </form>
      )}
      {!isAdmin && <p className="hint">Only administrators can add or deactivate equipment. Try switching user.</p>}
    </section>
  );
}
