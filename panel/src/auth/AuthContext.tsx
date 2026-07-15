import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { ApiError, apiRequest, login as loginRequest } from "../api";
import type { LoginResponse, Role } from "../types";

const STORAGE_KEY = "chargesquare.auth";

export interface AuthState {
  accessToken: string;
  username: string;
  role: Role;
  expiresAt: number;
}

interface AuthContextValue {
  auth: AuthState | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  request: <T>(path: string, options?: RequestInit) => Promise<T>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [auth, setAuth] = useState<AuthState | null>(() => readStoredAuth());

  const logout = useCallback(() => {
    sessionStorage.removeItem(STORAGE_KEY);
    setAuth(null);
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const response = await loginRequest(username, password);
    const nextAuth = toAuthState(response);
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(nextAuth));
    setAuth(nextAuth);
  }, []);

  const request = useCallback(
    async <T,>(path: string, options: RequestInit = {}): Promise<T> => {
      if (!auth) {
        throw new ApiError(401, "AUTHENTICATION_REQUIRED", "Authentication is required");
      }
      try {
        return await apiRequest<T>(path, options, auth.accessToken);
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
          logout();
        }
        throw error;
      }
    },
    [auth, logout],
  );

  useEffect(() => {
    if (!auth) return undefined;
    const remaining = auth.expiresAt - Date.now();
    if (remaining <= 0) {
      logout();
      return undefined;
    }
    const timer = window.setTimeout(logout, remaining);
    return () => window.clearTimeout(timer);
  }, [auth, logout]);

  const value = useMemo(() => ({ auth, login, logout, request }), [auth, login, logout, request]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}

function toAuthState(response: LoginResponse): AuthState {
  if (!response.accessToken || !["VIEWER", "ADMIN"].includes(response.role) || response.expiresIn <= 0) {
    throw new ApiError(500, "INVALID_LOGIN_RESPONSE", "The login response was invalid");
  }
  return {
    accessToken: response.accessToken,
    username: response.username,
    role: response.role,
    expiresAt: Date.now() + response.expiresIn * 1000,
  };
}

function readStoredAuth(): AuthState | null {
  const value = sessionStorage.getItem(STORAGE_KEY);
  if (!value) return null;
  try {
    const parsed = JSON.parse(value) as Partial<AuthState>;
    if (
      typeof parsed.accessToken !== "string" ||
      typeof parsed.username !== "string" ||
      !["VIEWER", "ADMIN"].includes(parsed.role ?? "") ||
      typeof parsed.expiresAt !== "number" ||
      parsed.expiresAt <= Date.now()
    ) {
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return parsed as AuthState;
  } catch {
    sessionStorage.removeItem(STORAGE_KEY);
    return null;
  }
}
