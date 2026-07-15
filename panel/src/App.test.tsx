import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AppRoutes } from "./App";
import { AuthProvider, type AuthState } from "./auth/AuthContext";
import type { ChargingSession, LoginResponse, StopSessionResponse } from "./types";

const activeSession: ChargingSession = {
  sessionId: 1,
  userId: 7,
  connectorId: 10,
  status: "ACTIVE",
  startedAt: "2026-07-15T08:00:00Z",
  endedAt: null,
  energyKwh: null,
  cost: null,
  tariffSnapshot: { pricePerKwh: 8.5, startFee: 2, currency: "TRY" },
};

const completedSession: ChargingSession = {
  ...activeSession,
  status: "COMPLETED",
  endedAt: "2026-07-15T08:45:00Z",
  energyKwh: 12.5,
  cost: 108.25,
};

const stopReceipt: StopSessionResponse = {
  sessionId: 1,
  userId: 7,
  connectorId: 10,
  status: "COMPLETED",
  startedAt: activeSession.startedAt,
  endedAt: completedSession.endedAt!,
  energyKwh: 12.5,
  cost: 108.25,
  currency: "TRY",
  walletBalanceAfter: 391.75,
};

describe("ChargeSquare panel", () => {
  let fetchMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
  });

  it("stores authenticated state after a successful login", async () => {
    const login: LoginResponse = {
      accessToken: "admin-token",
      tokenType: "Bearer",
      expiresIn: 900,
      username: "admin",
      role: "ADMIN",
    };
    fetchMock
      .mockResolvedValueOnce(jsonResponse(200, login))
      .mockResolvedValueOnce(jsonResponse(200, []));
    const user = userEvent.setup();
    renderAt("/login");

    await user.type(screen.getByLabelText("Username"), "admin");
    await user.type(screen.getByLabelText("Password"), "admin-demo");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("heading", { name: "Stations & connectors" })).toBeInTheDocument();
    const stored = JSON.parse(sessionStorage.getItem("chargesquare.auth") ?? "{}") as AuthState;
    expect(stored).toMatchObject({ accessToken: "admin-token", username: "admin", role: "ADMIN" });
    expect(sessionStorage.getItem("chargesquare.auth")).not.toContain("admin-demo");
  });

  it("shows invalid credentials without storing the password", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(401, {
      error: "INVALID_CREDENTIALS",
      message: "Invalid username or password",
    }));
    const user = userEvent.setup();
    renderAt("/login");

    await user.type(screen.getByLabelText("Username"), "viewer");
    await user.type(screen.getByLabelText("Password"), "wrong");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid username or password");
    expect(sessionStorage.getItem("chargesquare.auth")).toBeNull();
  });

  it("redirects an anonymous user to login", async () => {
    renderAt("/sessions");
    expect(await screen.findByRole("heading", { name: "Sign in to ChargeSquare" })).toBeInTheDocument();
  });

  it("lets VIEWER inspect an active receipt without an enabled stop action", async () => {
    storeAuth("VIEWER");
    fetchMock.mockResolvedValueOnce(jsonResponse(200, activeSession));
    renderAt("/sessions/1");

    expect(await screen.findByText("Read-only access")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Stop session" })).not.toBeInTheDocument();
  });

  it("shows the stop action to ADMIN", async () => {
    storeAuth("ADMIN");
    fetchMock.mockResolvedValueOnce(jsonResponse(200, activeSession));
    renderAt("/sessions/1");

    expect(await screen.findByRole("button", { name: "Stop session" })).toBeEnabled();
  });

  it("clears authentication and redirects when an API returns 401", async () => {
    storeAuth("VIEWER");
    fetchMock.mockResolvedValueOnce(jsonResponse(401, {
      error: "AUTHENTICATION_REQUIRED",
      message: "Authentication is required",
    }));
    renderAt("/connectors");

    expect(await screen.findByRole("heading", { name: "Sign in to ChargeSquare" })).toBeInTheDocument();
    expect(sessionStorage.getItem("chargesquare.auth")).toBeNull();
  });

  it("renders an access-denied message for 403 responses", async () => {
    storeAuth("VIEWER");
    fetchMock.mockResolvedValueOnce(jsonResponse(403, {
      error: "ACCESS_DENIED",
      message: "You do not have permission to perform this operation",
    }));
    renderAt("/connectors");

    expect(await screen.findByRole("alert")).toHaveTextContent("Access denied");
  });

  it("refreshes the receipt and shows settlement after a successful stop", async () => {
    storeAuth("ADMIN");
    fetchMock
      .mockResolvedValueOnce(jsonResponse(200, activeSession))
      .mockResolvedValueOnce(jsonResponse(200, stopReceipt))
      .mockResolvedValueOnce(jsonResponse(200, completedSession));
    const user = userEvent.setup();
    renderAt("/sessions/1");

    await user.type(await screen.findByLabelText("Energy delivered (kWh)"), "12.5");
    await user.click(screen.getByRole("button", { name: "Stop session" }));

    expect(await screen.findByText("108.25 TRY")).toBeInTheDocument();
    expect(screen.getByText("391.75 TRY")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Stop session" })).not.toBeInTheDocument();
  });

  it("renders a loading state while data is pending", () => {
    storeAuth("VIEWER");
    fetchMock.mockReturnValueOnce(new Promise(() => undefined));
    renderAt("/connectors");
    expect(screen.getByText("Loading connectors…")).toBeInTheDocument();
  });
});

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider><AppRoutes /></AuthProvider>
    </MemoryRouter>,
  );
}

function storeAuth(role: "VIEWER" | "ADMIN") {
  const auth: AuthState = {
    accessToken: `${role.toLowerCase()}-token`,
    username: role.toLowerCase(),
    role,
    expiresAt: Date.now() + 900_000,
  };
  sessionStorage.setItem("chargesquare.auth", JSON.stringify(auth));
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
