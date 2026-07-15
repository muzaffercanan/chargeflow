import { type FormEvent, useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError, messageForError } from "../api";
import { useAuth } from "../auth/AuthContext";
import { StatePanel } from "../components/StatePanel";
import { StatusBadge } from "../components/StatusBadge";
import type { ChargingSession, StopSessionResponse } from "../types";

export function SessionDetailPage() {
  const { sessionId } = useParams();
  const { auth, request } = useAuth();
  const [session, setSession] = useState<ChargingSession | null>(null);
  const [walletBalanceAfter, setWalletBalanceAfter] = useState<number | null>(null);
  const [energy, setEnergy] = useState("");
  const [loading, setLoading] = useState(true);
  const [stopping, setStopping] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const load = useCallback(async (preserveWallet = false) => {
    if (!sessionId) return;
    setLoading(true);
    setError(null);
    try {
      setSession(await request<ChargingSession>(`/api/session/sessions/${sessionId}`));
      if (!preserveWallet) setWalletBalanceAfter(null);
    } catch (caught) {
      setError(messageForError(caught));
    } finally {
      setLoading(false);
    }
  }, [request, sessionId]);

  useEffect(() => { void load(); }, [load]);

  async function stopSession(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setActionError(null);
    if (!/^\d+(\.\d{1,6})?$/.test(energy)) {
      setActionError("Enter zero or a positive value with at most six fractional digits.");
      return;
    }
    setStopping(true);
    try {
      const receipt = await request<StopSessionResponse>(`/api/session/sessions/${sessionId}/stop`, {
        method: "POST",
        body: JSON.stringify({ energyKwh: Number(energy) }),
      });
      setWalletBalanceAfter(receipt.walletBalanceAfter);
      await load(true);
    } catch (caught) {
      setActionError(caught instanceof ApiError && caught.status === 403
        ? "Access denied. Only ADMIN can stop a session."
        : messageForError(caught));
    } finally {
      setStopping(false);
    }
  }

  if (loading) return <StatePanel title="Loading receipt…" />;
  if (error) return <StatePanel title="Receipt error" message={error} actionLabel="Try again" onAction={() => void load()} />;
  if (!session) return <StatePanel title="Receipt unavailable" />;

  return (
    <>
      <Link className="back-link" to="/sessions">← All sessions</Link>
      <div className="page-heading">
        <div><span className="eyebrow">Session receipt</span><h1>Session #{session.sessionId}</h1></div>
        <StatusBadge status={session.status} />
      </div>
      <div className="detail-grid">
        <section className="detail-card">
          <h2>Charging record</h2>
          <dl>
            <div><dt>User</dt><dd>#{session.userId}</dd></div>
            <div><dt>Connector</dt><dd>#{session.connectorId}</dd></div>
            <div><dt>Started</dt><dd>{formatDate(session.startedAt)}</dd></div>
            <div><dt>Ended</dt><dd>{formatDate(session.endedAt)}</dd></div>
            <div><dt>Energy</dt><dd>{session.energyKwh === null ? "—" : `${session.energyKwh} kWh`}</dd></div>
          </dl>
        </section>
        <section className="detail-card">
          <h2>Tariff & settlement</h2>
          <dl>
            <div><dt>Energy price</dt><dd>{session.tariffSnapshot.pricePerKwh.toFixed(2)} {session.tariffSnapshot.currency}/kWh</dd></div>
            <div><dt>Start fee</dt><dd>{session.tariffSnapshot.startFee.toFixed(2)} {session.tariffSnapshot.currency}</dd></div>
            <div><dt>Final cost</dt><dd className="total">{session.cost === null ? "—" : `${session.cost.toFixed(2)} ${session.tariffSnapshot.currency}`}</dd></div>
            {walletBalanceAfter !== null && <div><dt>Wallet balance after</dt><dd>{walletBalanceAfter.toFixed(2)} {session.tariffSnapshot.currency}</dd></div>}
          </dl>
        </section>
      </div>
      {session.status === "ACTIVE" && auth?.role === "ADMIN" && (
        <section className="action-card">
          <div><span className="eyebrow">Admin action</span><h2>Stop active session</h2><p>Enter the meter-reported energy. This action bills the wallet and releases the connector.</p></div>
          <form onSubmit={stopSession}>
            <label htmlFor="energyKwh">Energy delivered (kWh)</label>
            <div className="action-row">
              <input id="energyKwh" inputMode="decimal" required value={energy} onChange={(event) => setEnergy(event.target.value)} />
              <button className="button danger" type="submit" disabled={stopping}>{stopping ? "Stopping…" : "Stop session"}</button>
            </div>
            {actionError && <div className="inline-error" role="alert">{actionError}</div>}
          </form>
        </section>
      )}
      {session.status === "ACTIVE" && auth?.role === "VIEWER" && (
        <StatePanel title="Read-only access" message="VIEWER can inspect this receipt but cannot stop the session." />
      )}
    </>
  );
}

function formatDate(value: string | null): string {
  return value ? new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "long" }).format(new Date(value)) : "—";
}
