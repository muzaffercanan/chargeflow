INSERT INTO ${station_schema}.stations (id, name)
VALUES (1, 'ChargeSquare Demo Station');

INSERT INTO ${station_schema}.tariffs (id, price_per_kwh, start_fee, currency)
VALUES (5, 8.50, 2.00, 'TRY');

INSERT INTO ${station_schema}.connectors (id, station_id, tariff_id, type, power_kw, status)
VALUES
    (10, 1, 5, 'CCS2-DC', 60, 'AVAILABLE'),
    (11, 1, 5, 'Type2-AC', 22, 'AVAILABLE');
