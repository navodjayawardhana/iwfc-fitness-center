// One small client for the REST API. After sign-in the token travels as: Authorization: Bearer <token>.

import type { LoginResult } from './types';

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE';

interface ErrorBody {
  error?: string;
  message?: string;
}

const TOKEN_KEY = 'fitpulse.token';

/** The token lives in sessionStorage: it survives a page reload but not closing the tab. */
export const tokenStore = {
  get(): string | null {
    try {
      return sessionStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  },
  save(token: string): void {
    try {
      sessionStorage.setItem(TOKEN_KEY, token);
    } catch {
      /* storage blocked: the user simply has to sign in again after a reload */
    }
  },
  clear(): void {
    try {
      sessionStorage.removeItem(TOKEN_KEY);
    } catch {
      /* nothing to clear */
    }
  },
};

let onSessionExpired: (() => void) | null = null;

/** The app registers what to do when the API says the token is no longer valid. */
export function setSessionExpiredHandler(handler: (() => void) | null): void {
  onSessionExpired = handler;
}

export async function call<T = unknown>(method: Method, path: string, body?: unknown): Promise<T> {
  const token = tokenStore.get();
  const response = await fetch(`/api${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (response.status === 204) return null as T;
  const data: unknown = await response.json().catch(() => null);
  if (!response.ok) {
    const error = (data ?? {}) as ErrorBody;
    if (response.status === 401 && token && path !== '/login') {
      tokenStore.clear();
      onSessionExpired?.();
    }
    throw new ApiError(response.status, error.error ?? 'ERROR', error.message ?? response.statusText);
  }
  return data as T;
}

export async function signIn(userId: string, password: string): Promise<LoginResult> {
  const result = await call<LoginResult>('POST', '/login', { userId, password });
  tokenStore.save(result.token);
  return result;
}

export async function signOut(): Promise<void> {
  try {
    await call('POST', '/logout');
  } catch {
    /* the token may already be invalid; signing out locally is enough */
  } finally {
    tokenStore.clear();
  }
}

// Quick-fill buttons on the login screen. Only the id is filled in: the password is never stored in the UI.
export const DEMO_USERS: { id: string; label: string }[] = [
  { id: 'A-1', label: 'Administrator' },
  { id: 'I-1', label: 'Instructor' },
  { id: 'M-1', label: 'Member' },
];

const TITLES: Record<string, string> = {
  UNAUTHORIZED_ACCESS: 'Access denied',
  INVALID_BOOKING: 'Invalid booking',
  DUPLICATE: 'Duplicate data',
  INVALID_STATUS_TRANSITION: 'Invalid workflow step',
  NOT_FOUND: 'Not found',
  BAD_REQUEST: 'Invalid input',
  INVALID_CREDENTIALS: 'Sign-in failed',
  MISSING_TOKEN: 'Sign-in needed',
};

export function describeError(error: unknown): string {
  if (error instanceof ApiError) {
    return `${TITLES[error.code] ?? 'Error'}: ${error.message}`;
  }
  const reason = error instanceof Error ? error.message : String(error);
  return `Could not reach the API (${reason}). Is the Spring Boot backend running on port 8080?`;
}
