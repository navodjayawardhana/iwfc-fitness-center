import { useState, type FormEvent } from 'react';
import { describeError, signIn } from '../api';
import PasswordField from './PasswordField';
import type { User } from '../types';
import { brandText, btn, card, field, muted } from '../ui';

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
    <div className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-4 py-10">
      <div className="mb-8 flex flex-col items-center text-center">
        <img src="/logo.svg" alt="" className="mb-4 h-16 w-auto drop-shadow-lg" />
        <h1 className="text-4xl font-extrabold tracking-tight">
          Fit<span className={brandText}>Pulse</span>
        </h1>
        <p className={`mt-1 ${muted}`}>Intelligent Wellness and Fitness Center</p>
      </div>

      <form className={`${card} flex flex-col gap-3 shadow-xl shadow-teal-900/5`} onSubmit={submit}>
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
          <PasswordField
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
    </div>
  );
}
