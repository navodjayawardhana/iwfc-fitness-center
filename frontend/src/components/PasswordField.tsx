import { useState, type InputHTMLAttributes } from 'react';
import { field } from '../ui';

type Props = Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'className'>;

/** A password input with an eye button that shows or hides what was typed. */
export default function PasswordField(props: Props) {
  const [visible, setVisible] = useState(false);
  return (
    <div className="relative min-w-0 flex-1">
      <input {...props} className={`${field} w-full pr-10`} type={visible ? 'text' : 'password'} />
      <button
        type="button"
        onClick={() => setVisible((current) => !current)}
        aria-label={visible ? 'Hide password' : 'Show password'}
        title={visible ? 'Hide password' : 'Show password'}
        className="absolute inset-y-0 right-0 flex w-10 cursor-pointer items-center justify-center text-slate-400 transition hover:text-teal-600 dark:hover:text-teal-300"
      >
        {visible ? (
          /* eye with a slash: the password is visible, click to hide */
          <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 10 8 10 8a18.5 18.5 0 0 1-2.16 3.19" />
            <path d="M6.61 6.61A13.53 13.53 0 0 0 2 12s3 8 10 8a9.74 9.74 0 0 0 5.39-1.61" />
            <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" />
            <line x1="2" y1="2" x2="22" y2="22" />
          </svg>
        ) : (
          /* open eye: the password is hidden, click to show */
          <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M2 12s3-8 10-8 10 8 10 8-3 8-10 8-10-8-10-8Z" />
            <circle cx="12" cy="12" r="3" />
          </svg>
        )}
      </button>
    </div>
  );
}
