CREATE DATABASE blynk_local_reporting;
USE blynk_local_reporting;

CREATE TABLE reporting_events (
  id UInt64,
  device_id UInt32,
  ts DateTime DEFAULT now(),
  event_hashcode Int32,
  type UInt8,
  description String
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(ts)
ORDER BY (device_id, ts, event_hashcode, type);

CREATE TABLE reporting_events_resolved (
  id UInt64,
  resolved_at DateTime DEFAULT now(),
  resolved_by String,
  resolved_comment String,
  is_resolved UInt8 DEFAULT 1
)
ENGINE = ReplacingMergeTree(resolved_at)
PARTITION BY toYYYYMM(resolved_at)
ORDER BY (id);

CREATE TABLE reporting_events_last_seen (
  device_id UInt32,
  email String,
  ts DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(ts)
ORDER BY (device_id, email);

CREATE TABLE reporting_device_raw_data (
  device_id UInt32,
  pin UInt8,
  pin_type UInt8,
  ts DateTime,
  value Float64
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(ts)
ORDER BY (device_id, pin, pin_type, ts);

CREATE MATERIALIZED VIEW reporting_average_minute
ENGINE = AggregatingMergeTree()
PARTITION BY toYYYYMM(ts)
ORDER BY (device_id, pin, pin_type, ts)
AS SELECT
    device_id,
    pin,
    pin_type,
    toStartOfMinute(ts) as ts,
    avgState(value) as value
FROM reporting_device_raw_data
GROUP BY device_id, pin, pin_type, ts;

CREATE MATERIALIZED VIEW reporting_average_5_minute
ENGINE = AggregatingMergeTree()
PARTITION BY toYYYYMM(ts)
ORDER BY (device_id, pin, pin_type, ts)
AS SELECT
    device_id,
    pin,
    pin_type,
    toStartOfFiveMinute(ts) as ts,
    avgState(value) as value
FROM reporting_device_raw_data
GROUP BY device_id, pin, pin_type, ts;

CREATE MATERIALIZED VIEW reporting_average_15_minute
ENGINE = AggregatingMergeTree()
PARTITION BY toYYYYMM(ts)
ORDER BY (device_id, pin, pin_type, ts)
AS SELECT
    device_id,
    pin,
    pin_type,
    toStartOfFifteenMinutes(ts) as ts,
    avgState(value) as value
FROM reporting_device_raw_data
GROUP BY device_id, pin, pin_type, ts;

CREATE MATERIALIZED VIEW reporting_average_hourly
ENGINE = AggregatingMergeTree()
PARTITION BY toYYYYMM(ts)
ORDER BY (device_id, pin, pin_type, ts)
AS SELECT
    device_id,
    pin,
    pin_type,
    toStartOfHour(ts) as ts,
    avgState(value) as value
FROM reporting_device_raw_data
GROUP BY device_id, pin, pin_type, ts;

CREATE MATERIALIZED VIEW reporting_average_daily
ENGINE = AggregatingMergeTree()
ORDER BY (device_id, pin, pin_type, ts)
AS SELECT
    device_id,
    pin,
    pin_type,
    toStartOfDay(ts) as ts,
    avgState(value) as value
FROM reporting_device_raw_data
GROUP BY device_id, pin, pin_type, ts;