import { useState, type FormEvent } from 'react';
import { call } from '../api';
import Drawer from './Drawer';
import PasswordField from './PasswordField';
import { useAction, useLoad } from '../hooks';
import type { Report, User } from '../types';
import { btn, btnDanger, chip, field, muted } from '../ui';

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
  const [adding, setAdding] = useState(false);

  const set = (key: keyof typeof form) => (event: { target: { value: string } }) => setForm({ ...form, [key]: event.target.value });

  const register = async (event: FormEvent) => {
    event.preventDefault();
    const created = await run(() => call('POST', '/users', form), `Registered ${form.id}`);
    if (created) {
      setForm({ id: '', name: '', role: 'MEMBER', password: '' });
      setAdding(false);
    }
  };

  return (
    <section>
      <div className="mt-2 mb-3 flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-xl font-semibold">User accounts</h2>
        <button className={btn} onClick={() => setAdding(true)}>
          + Register a user
        </button>
      </div>
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

      <Drawer open={adding} title="Register a user" onClose={() => setAdding(false)}>
        <form className="flex flex-col gap-3" onSubmit={(event) => void register(event)}>
          <label className="flex flex-col gap-1 text-sm">
            ID
            <input className={field} placeholder="e.g. M-3" value={form.id} onChange={set('id')} required />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            Full name
            <input className={field} value={form.name} onChange={set('name')} required />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            Role
            <select className={field} value={form.role} onChange={set('role')}>
              {ROLES.map((role) => (
                <option key={role}>{role}</option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1 text-sm">
            Password
            <PasswordField autoComplete="new-password" placeholder="8+ characters" value={form.password} onChange={set('password')} required />
          </label>
          <button className={`${btn} mt-2`} type="submit">
            Register
          </button>
        </form>
      </Drawer>
    </section>
  );
}
