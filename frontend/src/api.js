// One small client for the REST API. The acting user travels in the X-User-Id header (demo-level identity).

export class ApiError extends Error {
  constructor(status, code, message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

export async function call(method, path, userId, body) {
  const response = await fetch(`/api${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(userId ? { 'X-User-Id': userId } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (response.status === 204) return null;
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(response.status, data?.error ?? 'ERROR', data?.message ?? response.statusText);
  }
  return data;
}

// Demo accounts that exist in the seeded data.
export const DEMO_USERS = [
  { id: 'A-1', label: 'Amal Perera (Administrator)' },
  { id: 'I-1', label: 'Nimali Silva (Instructor)' },
  { id: 'I-2', label: 'Kasun Fernando (Instructor)' },
  { id: 'M-1', label: 'Dilani Jayasinghe (Member)' },
  { id: 'M-2', label: 'Ruwan Bandara (Member)' },
];

const TITLES = {
  UNAUTHORIZED_ACCESS: 'Access denied',
  INVALID_BOOKING: 'Invalid booking',
  DUPLICATE: 'Duplicate data',
  INVALID_STATUS_TRANSITION: 'Invalid workflow step',
  NOT_FOUND: 'Not found',
  BAD_REQUEST: 'Invalid input',
};

export function describeError(error) {
  if (error instanceof ApiError) {
    return `${TITLES[error.code] ?? 'Error'}: ${error.message}`;
  }
  return `Could not reach the API (${error.message}). Is the Spring Boot server running on port 8080?`;
}
