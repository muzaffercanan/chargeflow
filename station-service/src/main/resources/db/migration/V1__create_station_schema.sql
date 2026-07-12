CREATE SCHEMA IF NOT EXISTS ${station_schema};

CREATE TABLE ${station_schema}.stations (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE ${station_schema}.tariffs (
    id BIGINT PRIMARY KEY,
    price_per_kwh NUMERIC(19, 2) NOT NULL,
    start_fee NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL
);

CREATE TABLE ${station_schema}.connectors (
    id BIGINT PRIMARY KEY,
    station_id BIGINT NOT NULL REFERENCES ${station_schema}.stations (id),
    tariff_id BIGINT NOT NULL REFERENCES ${station_schema}.tariffs (id),
    type VARCHAR(100) NOT NULL,
    power_kw INTEGER NOT NULL CHECK (power_kw > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'OCCUPIED'))
);

CREATE INDEX connectors_station_id_idx ON ${station_schema}.connectors (station_id);
