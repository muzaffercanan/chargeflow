# ChargeSquare Security Design

This document describes the implemented take-home security boundary. It is intentionally small and demonstrative, not production-ready security.

## Authentication

`POST /auth/login` is the only public authentication endpoint. Session Service looks up one of the two persisted demo users, verifies the submitted password with BCrypt, and returns a short-lived HS256 JWT on success. Password hashes are stored in the `session.auth_users` table; plaintext passwords are never persisted.

Human access tokens contain the subject, one `VIEWER` or `ADMIN` role, issued-at time, expiry, issuer, audience, and token type. Both services verify the HS256 signature, issuer, audience, and standard time claims. The issuer, audience, and human-token lifetime are environment-backed (`JWT_ISSUER`, `JWT_AUDIENCE`, and `JWT_ACCESS_TTL`; 15 minutes locally). Clients carry the token as `Authorization: Bearer <token>`.

`JWT_SIGNING_SECRET` is read from the environment and from a Kubernetes `secretKeyRef`, never Java, SQL, a ConfigMap, or committed YAML. HS256 makes the secret both signing and verification material, so distributing it through configuration would expose the entire trust boundary.

## Authorization

| Capability | VIEWER | ADMIN | SERVICE |
| --- | --- | --- | --- |
| Login | Yes | Yes | No |
| View stations/connectors | Yes | Yes | No |
| View sessions/receipts | Yes | Yes | No |
| Start/stop session | No | Yes | No |
| Internal occupy/release | No | Optional direct admin | Yes |

Spring Security enforces these rules in both backends. Hiding or disabling a panel button is only a user-experience convenience; it is not an authorization control. A caller that bypasses or modifies the panel still receives backend `401 AUTHENTICATION_REQUIRED` or `403 ACCESS_DENIED` responses.

## Service-to-service authentication

Session Service creates a fresh short-lived JWT with subject `session-service` and role `SERVICE` for each occupy/release call. Station Service verifies it with the same signature, issuer, audience, and expiry checks used for human tokens. The token lifetime is separately configured by `JWT_SERVICE_TTL` (60 seconds locally), and browser users never receive this credential.

The ADMIN bearer token is delegated only for the connector read that Station Service already protects as a human read. A separate SERVICE identity is used for connector mutation so a browser credential is not treated as a service credential. This preserves the existing two-service topology and narrow REST boundary without adding a gateway. It also avoids forwarding browser tokens to privileged internal operations.

## Browser token storage

The React panel stores the access token, username, role, and calculated expiry in `sessionStorage`; it never stores the password. This supports same-tab refresh and limits persistence compared with `localStorage`, but any successful XSS can still read and exfiltrate the token.

A production deployment would normally consider an `HttpOnly`, `Secure`, `SameSite` cookie or a backend-for-frontend (BFF). That reduces JavaScript token exposure but introduces cookie scope and CSRF protections; those trade-offs are deliberately not simulated here. The current bearer-token transport is not automatically attached cross-site, so it has less ambient CSRF exposure than cookie authentication.

## CORS and input trust

Each backend allows one environment-configured origin (`PANEL_ALLOWED_ORIGIN`), only the required `GET`, `POST`, and `OPTIONS` methods, and only `Authorization` and `Content-Type` headers. Wildcard origins and browser credentials are disabled. The Compose Nginx panel normally uses same-origin reverse proxies.

Panel validation improves feedback, but it is never trusted as a security boundary: anyone can bypass JavaScript and call the APIs directly. Bean Validation and lifecycle/authorization checks therefore run again in the owning backend.

## Secrets

No signing key, database password, token, or other live secret value is stored in git. `.env.example` contains local demonstration placeholders; real local values belong in the ignored `.env` file or process environment. Kubernetes Deployments obtain database credentials and `JWT_SIGNING_SECRET` through `secretKeyRef`; the repository intentionally contains no Secret resource or value.

A production deployment should use the platform's secret manager, restrict secret access to the two services, generate high-entropy values, and rotate them through a controlled rollout. Automated key rotation is future work.

## Audit and operational logging

Application logs use focused `key=value` fields and the logging framework supplies the timestamp. Recorded events include:

- `event=login_succeeded` and `event=login_failed`, with the actor/attempted username and role where known;
- `event=session_started` and `event=session_stopped`, with actor, role, session, user, connector, energy, cost, and currency as relevant;
- `event=wallet_debited`, `event=connector_occupied`, and `event=connector_released`, with the relevant resource identifiers and settlement fields;
- useful backend authorization failures, with actor, role, method, and path;
- `event=station_dependency_timeout` when the bounded Station call times out.

Passwords, password hashes, JWTs, signing secrets, SERVICE credentials, `Authorization` headers, and request headers are never logged. These container logs are useful for a reviewer and basic operations, but they are not an immutable or queryable compliance audit store.

## Honest limitations

This take-home does not implement refresh tokens, signup, password reset, account lockout, token revocation, an OAuth/OIDC identity provider, automated signing-key rotation, advanced audit-log storage, or rate limiting. It also does not claim protection from every XSS issue, immediate role/disable revocation, production secret lifecycle management, or production-grade identity assurance.
