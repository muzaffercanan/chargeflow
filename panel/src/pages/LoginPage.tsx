import { type FormEvent, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { ApiError } from "../api";
import { useAuth } from "../auth/AuthContext";

export function LoginPage() {
  const { auth, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (auth) return <Navigate to="/connectors" replace />;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await login(username, password);
      const destination = (location.state as { from?: string } | null)?.from ?? "/connectors";
      navigate(destination, { replace: true });
    } catch (caught) {
      setError(caught instanceof ApiError && caught.status === 401
        ? "Invalid username or password."
        : "Login is unavailable. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-page">
      <section className="login-card" aria-labelledby="login-heading">
        <div className="login-mark" aria-hidden="true">CS</div>
        <span className="eyebrow">Operations console</span>
        <h1 id="login-heading">Sign in to ChargeSquare</h1>
        <p>Monitor connectors, review charging receipts, and manage active sessions.</p>
        <form onSubmit={submit}>
          <label htmlFor="username">Username</label>
          <input
            id="username"
            name="username"
            autoComplete="username"
            required
            value={username}
            onChange={(event) => setUsername(event.target.value)}
          />
          <label htmlFor="password">Password</label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
          {error && <div className="inline-error" role="alert">{error}</div>}
          <button className="button primary wide" type="submit" disabled={loading}>
            {loading ? "Signing in…" : "Sign in"}
          </button>
        </form>
        <p className="login-note">Use the documented local VIEWER or ADMIN demo account.</p>
      </section>
    </main>
  );
}
