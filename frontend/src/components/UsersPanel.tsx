import { useState, type FormEvent } from 'react';
import { call } from '../api';
import { useAction, useLoad } from '../hooks';
import type { Report, User } from '../types';
import { btn, btnDanger, card, chip, field, muted } from '../ui';

const ROLES = ['MEMBER', 'INSTRUCTOR', 'ADMINISTRATOR'];

interface Props {
  user: User;
  report: Report;
}

/** Administrators oversee user accounts: list, register and deactivate. */
export default function UsersPanel({ user, report }: Props) {
  const { data: users, reload } = useLoad(() => call<User[]>('GET', '/users'), [user.id], report);
  const run = useAction(report, reload);
  const [form, setForm] = useState({ id: '', name: '', role: 'MEMBER', password: '' });

  const set = (key: keyof typeof form) => (event: { target: { value: string } }) => setForm({ ...form, [key]: event.target.value });

  const register = async (event: FormEvent) => {
    event.preventDefault();
    const created = await run(() => call('POST', '/users', form), `Registered ${form.id}`);
    if (created) setForm({ id: '', name: '', role: 'MEMBER', password: '' });
  };

  return (
    <section>
      <h2 className="mt-2 mb-3 text-xl font-semibold">User accounts</h2>
      <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-900">
        <table className="w-full border-collapse text-left text-sm">
          <thead>
            <tr className={`text-xs tracking-wide uppercase ${muted}`}>
              <th className="p-3">ID</th>
              <th className="p-3">Name</th>
              <th className="p-3">Role</th>
              <th className="p-3">Status</th>
              <th className="p-3">Actions</th>
            </tr>
          </thead>
          <tbody>
            {(users ?? []).map((account) => (
              <tr key={account.id} className={`border-t border-slate-200 dark:border-slate-700 ${account.active ? '' : 'opacity-55'}`}>
                <td className="p-3">{account.id}</td>
                <td className="p-3">{account.name}</td>
                <td className="p-3">{account.role}</td>
                <td className="p-3">
                  <span className={account.active ? chip.good : chip.neutral}>{account.active ? 'ACTIVE' : 'DEACTIVATED'}</span>
                </td>
                <td className="p-3">
                  {account.active && account.id !== user.id && (
                    <button className={btnDanger} onClick={() => void run(() => call('POST', `/users/${account.id}/deactivate`), `Deactivated ${account.id}`)}>
                      Deactivate
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <form className={`${card} mt-4 flex flex-wrap items-center gap-2`} onSubmit={register}>
        <h3 className="w-full font-semibold">Register a user</h3>
        <input className={field} placeholder="ID (e.g. M-3)" value={form.id} onChange={set('id')} />
        <input className={field} placeholder="Full name" value={form.name} onChange={set('name')} />
        <select className={field} value={form.role} onChange={set('role')}>
          {ROLES.map((role) => (
            <option key={role}>{role}</option>
          ))}
        </select>
        <input className={field} type="password" autoComplete="new-password" placeholder="Password (8+ characters)" value={form.password} onChange={set('password')} />
        <button className={btn} type="submit">
          Register
        </button>
      </form>
    </section>
  );
}
