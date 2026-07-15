import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function AppLayout() {
  const { auth, logout } = useAuth();
  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <span className="eyebrow">Operations console</span>
          <strong className="brand">ChargeSquare</strong>
        </div>
        <nav aria-label="Primary navigation">
          <NavLink to="/connectors">Connectors</NavLink>
          <NavLink to="/sessions">Sessions</NavLink>
        </nav>
        <div className="account">
          <span>{auth?.username}</span>
          <span className="role-chip">{auth?.role}</span>
          <button className="button ghost" type="button" onClick={logout}>Log out</button>
        </div>
      </header>
      <main className="page"><Outlet /></main>
    </div>
  );
}
