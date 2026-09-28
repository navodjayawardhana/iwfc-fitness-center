import { useState, type FormEvent } from 'react';
import { DEMO_USERS, describeError, signIn } from '../api';
import type { User } from '../types';
import { btn, btnGhost, card, field, muted } from '../ui';

interface Props {
  onSignedIn: (user: User) => void;
}

export default function LoginScreen({ onSignedIn }: Props) {
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const result = await signIn(userId.trim(), password);
      setPassword('');
      onSignedIn(result.user);
    } catch (failure) {
      setError(describeError(failure));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-4">
      <h1 className="text-3xl font-bold tracking-wide">FitPulse</h1>
      <p className={`mb-6 ${muted}`}>Intelligent Wellness and Fitness Center</p>

      <form className={`${card} flex flex-col gap-3`} onSubmit={submit}>
        <h2 className="text-lg font-semibold">Sign in</h2>
        <label className="flex flex-col gap-1 text-sm">
          User id
          <input
            className={field}
            autoComplete="username"
            value={userId}
            onChange={(event) => setUserId(event.target.value)}
            required
          />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          Password
          <input
            className={field}
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </label>
        {error && (
          <p role="alert" className="rounded-lg bg-red-100 px-3 py-2 text-sm text-red-900 dark:bg-red-950 dark:text-red-200">
            {error}
          </p>
        )}
        <button className={btn} type="submit" disabled={busy}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
      </form>

      <div className={`mt-4 text-sm ${muted}`}>
        <p className="mb-2">Demo accounts (the demo password is in the README):</p>
        <div className="flex flex-wrap gap-2">
          {DEMO_USERS.map((demo) => (
            <button key={demo.id} type="button" className={btnGhost} onClick={() => setUserId(demo.id)}>
              {demo.label} ({demo.id})
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
