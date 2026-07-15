export type Role = "VIEWER" | "ADMIN";

export interface LoginResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  username: string;
  role: Role;
}

export interface Tariff {
  tariffId: number;
  pricePerKwh: number;
  startFee: number;
  currency: string;
}

export interface Connector {
  connectorId: number;
  stationId: number;
  type: string;
  powerKw: number;
  status: "AVAILABLE" | "OCCUPIED";
  tariff: Tariff;
}

export interface TariffSnapshot {
  pricePerKwh: number;
  startFee: number;
  currency: string;
}

export interface ChargingSession {
  sessionId: number;
  userId: number;
  connectorId: number;
  status: "ACTIVE" | "COMPLETED";
  startedAt: string;
  endedAt: string | null;
  energyKwh: number | null;
  cost: number | null;
  tariffSnapshot: TariffSnapshot;
}

export interface StopSessionResponse {
  sessionId: number;
  userId: number;
  connectorId: number;
  status: "COMPLETED";
  startedAt: string;
  endedAt: string;
  energyKwh: number;
  cost: number;
  currency: string;
  walletBalanceAfter: number;
}
