# ADR 0002: Use local JSON storage for v1

- Status: Accepted
- Date: 2026-08-19

## Context

TempoTrack stores modest, user-owned stopwatch session data and has no server. A database would add migration/runtime complexity without evidence that v1 query volume requires it.

## Decision

Use application-private JSON files behind repository interfaces. Platform adapters perform filesystem I/O and temporary-file replacement where available.

## Consequences

The data remains human-portable and easy to export. Repository interfaces allow a future database migration without coupling UI/domain logic to the storage format.

A database migration should be introduced only when profiling or feature requirements justify it.
