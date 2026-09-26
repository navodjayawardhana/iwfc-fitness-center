// One small client for the REST API. The acting user travels in the X-User-Id header (demo-level identity).

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

export async function call<T = unknown>(method: Method, path: string, userId: string | null, body?: unknown): Promise<T> {
  const response = await fetch(`/api${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(userId ? { 'X-User-Id': userId } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (response.status === 204) return null as T;
  const data: unknown = await response.json().catch(() => null);
  if (!response.ok) {
    const error = (data ?? {}) as ErrorBody;
    throw new ApiError(response.status, error.error ?? 'ERROR', error.message ?? response.statusText);
  }
  return data as T;
}

// Demo accounts that exist in the seeded data.
export const DEMO_USERS: { id: string; label: string }[] = [
  { id: 'A-1', label: 'Amal Perera (Administrator)' },
  { id: 'I-1', label: 'Nimali Silva (Instructor)' },
  { id: 'I-2', label: 'Kasun Fernando (Instructor)' },
  { id: 'M-1', label: 'Dilani Jayasinghe (Member)' },
  { id: 'M-2', label: 'Ruwan Bandara (Member)' },
];

const TITLES: Record<string, string> = {
  UNAUTHORIZED_ACCESS: 'Access denied',
  INVALID_BOOKING: 'Invalid booking',
  DUPLICATE: 'Duplicate data',
  INVALID_STATUS_TRANSITION: 'Invalid workflow step',
  NOT_FOUND: 'Not found',
  BAD_REQUEST: 'Invalid input',
};

export function describeError(error: unknown): string {
  if (error instanceof ApiError) {
    return `${TITLES[error.code] ?? 'Error'}: ${error.message}`;
  }
  const reason = error instanceof Error ? error.message : String(error);
  return `Could not reach the API (${reason}). Is the Spring Boot backend running on port 8080?`;
}
