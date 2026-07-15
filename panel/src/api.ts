import type { LoginResponse } from "./types";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
  }
}

interface ErrorBody {
  error?: string;
  message?: string;
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
  accessToken?: string,
): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body) {
    headers.set("Content-Type", "application/json");
  }
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  let response: Response;
  try {
    response = await fetch(path, { ...options, headers });
  } catch {
    throw new ApiError(0, "NETWORK_ERROR", "The service could not be reached");
  }

  if (!response.ok) {
    const body = await readError(response);
    throw new ApiError(
      response.status,
      body.error ?? "REQUEST_FAILED",
      body.message ?? `Request failed with status ${response.status}`,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export function login(username: string, password: string): Promise<LoginResponse> {
  return apiRequest<LoginResponse>("/api/session/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
}

export function messageForError(error: unknown): string {
  if (!(error instanceof ApiError)) {
    return "An unexpected error occurred.";
  }
  switch (error.status) {
    case 0:
      return "The service is unavailable. Check your connection and try again.";
    case 400:
      return "Check the entered value and try again.";
    case 401:
      return "Your session has expired. Please sign in again.";
    case 403:
      return "Access denied. Your role cannot perform this operation.";
    case 404:
      return "The requested record was not found.";
    case 409:
      return "The record changed and this action can no longer be completed.";
    case 503:
      return "A required service is temporarily unavailable. Try again shortly.";
    default:
      return error.message;
  }
}

async function readError(response: Response): Promise<ErrorBody> {
  try {
    return (await response.json()) as ErrorBody;
  } catch {
    return {};
  }
}
