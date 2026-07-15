import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { messageForError } from "../api";
import { useAuth } from "../auth/AuthContext";
import { StatePanel } from "../components/StatePanel";
import { StatusBadge } from "../components/StatusBadge";
import type { ChargingSession } from "../types";

const userId = import.meta.env.VITE_DEMO_USER_ID ?? "7";

export function SessionsPage() {
  const { request } = useAuth();
  const [sessions, setSessions] = useState<ChargingSession[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setSessions(null);
    setError(null);
    try {
      setSessions(await request<ChargingSession[]>(`/api/session/users/${userId}/sessions`));
    } catch (caught) {
      setError(messageForError(caught));
    }
  }, [request]);

  useEffect(() => { void load(); }, [load]);

  return (
    <>
      <div className="page-heading">
        <div><span className="eyebrow">Demo user {userId}</span><h1>Charging sessions</h1></div>
        <button className="button secondary" type="button" onClick={() => void load()}>Refresh</button>
      </div>
      {sessions === null && !error && <StatePanel title="Loading sessions…" />}
      {error && <StatePanel title="Session error" message={error} actionLabel="Try again" onAction={() => void load()} />}
      {sessions?.length === 0 && <StatePanel title="No sessions yet" message="Start a session through the authenticated API to see it here." />}
      {sessions && sessions.length > 0 && (
        <div className="table-card">
          <table>
            <thead><tr><th>Session</th><th>User</th><th>Connector</th><th>Status</th><th>Started</th><th>Ended</th><th>Energy</th><th>Cost</th></tr></thead>
            <tbody>
              {sessions.map((session) => (
                <tr key={session.sessionId}>
                  <td><Link to={`/sessions/${session.sessionId}`}>#{session.sessionId}</Link></td>
                  <td>#{session.userId}</td>
                  <td>#{session.connectorId}</td>
                  <td><StatusBadge status={session.status} /></td>
                  <td>{formatDate(session.startedAt)}</td>
                  <td>{formatDate(session.endedAt)}</td>
                  <td>{session.energyKwh === null ? "—" : `${session.energyKwh} kWh`}</td>
                  <td>{session.cost === null ? "—" : `${session.cost.toFixed(2)} ${session.tariffSnapshot.currency}`}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

function formatDate(value: string | null): string {
  return value ? new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "—";
}
