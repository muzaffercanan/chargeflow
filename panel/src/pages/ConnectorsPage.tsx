import { useCallback, useEffect, useState } from "react";
import { messageForError } from "../api";
import { useAuth } from "../auth/AuthContext";
import { StatePanel } from "../components/StatePanel";
import { StatusBadge } from "../components/StatusBadge";
import type { Connector } from "../types";

const stationId = import.meta.env.VITE_DEMO_STATION_ID ?? "1";

export function ConnectorsPage() {
  const { request } = useAuth();
  const [connectors, setConnectors] = useState<Connector[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    setConnectors(null);
    try {
      setConnectors(await request<Connector[]>(`/api/station/stations/${stationId}/connectors`));
    } catch (caught) {
      setError(messageForError(caught));
    }
  }, [request]);

  useEffect(() => { void load(); }, [load]);

  return (
    <>
      <div className="page-heading">
        <div><span className="eyebrow">Station {stationId}</span><h1>Stations & connectors</h1></div>
        <button className="button secondary" type="button" onClick={() => void load()}>Refresh</button>
      </div>
      {connectors === null && !error && <StatePanel title="Loading connectors…" />}
      {error && <StatePanel title="Connector error" message={error} actionLabel="Try again" onAction={() => void load()} />}
      {connectors?.length === 0 && <StatePanel title="No connectors" message="This station has no connectors." />}
      {connectors && connectors.length > 0 && (
        <div className="table-card">
          <table>
            <thead><tr><th>Station</th><th>Connector</th><th>Type</th><th>Power</th><th>Status</th><th>Tariff</th><th>Start fee</th></tr></thead>
            <tbody>
              {connectors.map((connector) => (
                <tr key={connector.connectorId}>
                  <td>#{connector.stationId}</td>
                  <td><strong>#{connector.connectorId}</strong></td>
                  <td>{connector.type}</td>
                  <td>{connector.powerKw} kW</td>
                  <td><StatusBadge status={connector.status} /></td>
                  <td>{connector.tariff.pricePerKwh.toFixed(2)} {connector.tariff.currency}/kWh</td>
                  <td>{connector.tariff.startFee.toFixed(2)} {connector.tariff.currency}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
