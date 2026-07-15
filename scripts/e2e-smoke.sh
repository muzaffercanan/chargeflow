#!/usr/bin/env bash

set -Eeuo pipefail

STATION_URL="${STATION_URL:-http://localhost:8081}"
SESSION_URL="${SESSION_URL:-http://localhost:8082}"
PANEL_URL="${PANEL_URL:-http://localhost:5173}"
DEMO_USER_ID="${DEMO_USER_ID:-7}"
DEMO_STATION_ID="${DEMO_STATION_ID:-1}"
EXPECTED_COST="${EXPECTED_COST:-108.25}"
EXPECTED_WALLET_BALANCE="${EXPECTED_WALLET_BALANCE:-391.75}"

for command_name in curl jq; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "ERROR: required command '$command_name' is not installed" >&2
    exit 1
  fi
done

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
LAST_BODY="$TMP_DIR/response.json"
LAST_STATUS=""

fail() {
  echo "ERROR: $*" >&2
  if [[ -s "$LAST_BODY" ]]; then
    echo "Last response body:" >&2
    cat "$LAST_BODY" >&2
    echo >&2
  fi
  exit 1
}

request() {
  local method="$1"
  local url="$2"
  local token="${3:-}"
  local data="${4:-}"
  local -a arguments=(-sS -o "$LAST_BODY" -w '%{http_code}' -X "$method" "$url")

  if [[ -n "$token" ]]; then
    arguments+=(-H "Authorization: Bearer $token")
  fi
  if [[ -n "$data" ]]; then
    arguments+=(-H 'Content-Type: application/json' --data "$data")
  fi

  LAST_STATUS="$(curl "${arguments[@]}")"
}

expect_status() {
  local expected="$1"
  local description="$2"
  if [[ "$LAST_STATUS" != "$expected" ]]; then
    fail "$description returned HTTP $LAST_STATUS; expected $expected"
  fi
}

expect_json() {
  local filter="$1"
  local description="$2"
  if ! jq -e "$filter" "$LAST_BODY" >/dev/null; then
    fail "$description did not match: $filter"
  fi
}

echo "Checking backend and panel health"
request GET "$STATION_URL/health"
expect_status 200 "Station health"
expect_json '.status == "UP"' "Station health body"

request GET "$SESSION_URL/health"
expect_status 200 "Session health"
expect_json '.status == "UP"' "Session health body"

request GET "$PANEL_URL/health"
expect_status 200 "Panel health"
if ! grep -qx 'ok' "$LAST_BODY"; then
  fail "Panel health body was not 'ok'"
fi

echo "Checking VIEWER read and write authorization"
request POST "$SESSION_URL/auth/login" "" '{"username":"viewer","password":"viewer-demo"}'
expect_status 200 "VIEWER login"
expect_json '.role == "VIEWER" and (.accessToken | type == "string" and length > 0)' "VIEWER login body"
VIEWER_TOKEN="$(jq -er '.accessToken' "$LAST_BODY")"

request GET "$STATION_URL/stations/$DEMO_STATION_ID/connectors" "$VIEWER_TOKEN"
expect_status 200 "VIEWER connector read"
expect_json 'type == "array" and length >= 1' "VIEWER connector response"
VIEWER_CONNECTOR_ID="$(jq -er 'map(select(.status == "AVAILABLE")) | first | .connectorId' "$LAST_BODY")"

request POST "$SESSION_URL/sessions" "$VIEWER_TOKEN" \
  "{\"userId\":$DEMO_USER_ID,\"connectorId\":$VIEWER_CONNECTOR_ID}"
expect_status 403 "VIEWER session start"
expect_json '.error == "ACCESS_DENIED"' "VIEWER forbidden response"

echo "Checking authenticated cross-service start, bill, settle, and release"
request POST "$SESSION_URL/auth/login" "" '{"username":"admin","password":"admin-demo"}'
expect_status 200 "ADMIN login"
expect_json '.role == "ADMIN" and (.accessToken | type == "string" and length > 0)' "ADMIN login body"
ADMIN_TOKEN="$(jq -er '.accessToken' "$LAST_BODY")"

request GET "$STATION_URL/stations/$DEMO_STATION_ID/connectors" "$ADMIN_TOKEN"
expect_status 200 "ADMIN connector read"
CONNECTOR_ID="$(jq -er 'map(select(.status == "AVAILABLE")) | first | .connectorId' "$LAST_BODY")"
if ! jq -e --argjson connectorId "$CONNECTOR_ID" '
  .[] | select(.connectorId == $connectorId) |
  .tariff.pricePerKwh == 8.5 and .tariff.startFee == 2 and .tariff.currency == "TRY"
' "$LAST_BODY" >/dev/null; then
  fail "Available connector does not have the deterministic 8.50 + 2.00 TRY tariff"
fi

request POST "$SESSION_URL/sessions" "$ADMIN_TOKEN" \
  "{\"userId\":$DEMO_USER_ID,\"connectorId\":$CONNECTOR_ID}"
expect_status 201 "ADMIN session start"
if ! jq -e --argjson userId "$DEMO_USER_ID" --argjson connectorId "$CONNECTOR_ID" '
  .status == "ACTIVE" and .userId == $userId and .connectorId == $connectorId and
  (.sessionId | type == "number")
' "$LAST_BODY" >/dev/null; then
  fail "Session start response did not match the selected user and connector"
fi
SESSION_ID="$(jq -er '.sessionId' "$LAST_BODY")"

request GET "$STATION_URL/connectors/$CONNECTOR_ID" "$ADMIN_TOKEN"
expect_status 200 "Occupied connector read"
expect_json '.status == "OCCUPIED"' "Connector occupation"

request POST "$SESSION_URL/sessions/$SESSION_ID/stop" "$ADMIN_TOKEN" '{"energyKwh":12.5}'
expect_status 200 "Session stop"
if ! jq -e --argjson sessionId "$SESSION_ID" --argjson expectedCost "$EXPECTED_COST" \
  --argjson expectedBalance "$EXPECTED_WALLET_BALANCE" '
  .sessionId == $sessionId and .status == "COMPLETED" and .energyKwh == 12.5 and
  .cost == $expectedCost and .currency == "TRY" and .walletBalanceAfter == $expectedBalance
' "$LAST_BODY" >/dev/null; then
  fail "Stop receipt did not contain the expected 108.25 cost and 391.75 wallet balance"
fi

request GET "$STATION_URL/connectors/$CONNECTOR_ID" "$ADMIN_TOKEN"
expect_status 200 "Released connector read"
expect_json '.status == "AVAILABLE"' "Connector release"

request POST "$SESSION_URL/sessions/$SESSION_ID/stop" "$ADMIN_TOKEN" '{"energyKwh":12.5}'
expect_status 409 "Second session stop"
expect_json '.error == "SESSION_NOT_ACTIVE"' "Second-stop conflict"

request GET "$SESSION_URL/sessions/$SESSION_ID" "$ADMIN_TOKEN"
expect_status 200 "Completed session read"
if ! jq -e --argjson sessionId "$SESSION_ID" --argjson expectedCost "$EXPECTED_COST" '
  .sessionId == $sessionId and .status == "COMPLETED" and .energyKwh == 12.5 and
  .cost == $expectedCost and .tariffSnapshot.pricePerKwh == 8.5 and .tariffSnapshot.startFee == 2
' "$LAST_BODY" >/dev/null; then
  fail "Completed session read did not match the persisted receipt"
fi

request GET "$SESSION_URL/users/$DEMO_USER_ID/sessions" "$VIEWER_TOKEN"
expect_status 200 "VIEWER session-history read"
if ! jq -e --argjson sessionId "$SESSION_ID" 'any(.[]; .sessionId == $sessionId and .status == "COMPLETED")' \
  "$LAST_BODY" >/dev/null; then
  fail "Completed session was not present in the user's session history"
fi

echo "E2E smoke test passed: sessionId=$SESSION_ID connectorId=$CONNECTOR_ID cost=$EXPECTED_COST wallet=$EXPECTED_WALLET_BALANCE"
