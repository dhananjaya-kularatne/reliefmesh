# ReliefMesh

[![CI](https://github.com/dhananjaya-kularatne/reliefmesh/actions/workflows/ci.yml/badge.svg?branch=dev)](https://github.com/dhananjaya-kularatne/reliefmesh/actions/workflows/ci.yml)

An offline-first backend for disaster relief coordination that keeps working when networks fail.

Volunteers record rescue requests, shelter status, supply needs and missing persons with no connectivity at all. Data syncs to district hubs whenever any connection is available, and hubs sync with each other when links between districts come back.

## Why

During Cyclone Ditwah (Sri Lanka, November 2025), power outages and communication breakdowns made coordination tools that assume constant connectivity fail exactly when they were needed most. ReliefMesh is designed for that situation: disconnection is the normal case, not an error.

## How it works

ReliefMesh treats data in two different ways.

| Kind of data | Examples | Approach |
|---|---|---|
| Mergeable | Reports, shelter details, notes, missing persons | Replicated with CRDTs. Every replica accepts writes while disconnected, and all replicas converge to the same state after syncing. |
| Scarce resources | Boats, ambulances, limited supply stock | Must never be allocated twice or beyond what exists. Connected hubs allocate through a Raft-replicated service. A disconnected hub allocates only from its own pre-assigned quota, so the global total can never be exceeded. |

## Modules

| Module | Purpose |
|---|---|
| `crdt` | CRDT library: hybrid logical clock, version vectors, registers, counters, sets, maps, delta-state |
| `sync` | Delta sync protocol: batching, compression, resumable transfers, priority ordering, idempotency |
| `hub-core` | Hub logic in plain Java: merging, syncing and hub-to-hub anti-entropy |
| `hub` | District hub service: Spring Boot, PostgreSQL, gRPC |
| `sdk` | Client SDK: local SQLite store, offline queue, sync client |
| `simulator` | Simulates thousands of volunteer devices going offline and online |
| `raft` | Raft consensus: leader election, log replication, persistence, snapshots |
| `allocation` | Resource allocation on Raft, with escrow quotas for disconnected hubs |
| `sms` | SMS gateway for structured text reports such as `RESCUE 4 KADUWELA ROOF` |
| `harness` | Deterministic simulation, fault injection and correctness checkers |

## Building

Requirements: Java 21 or newer. Maven does not need to be installed; the included wrapper downloads the correct version.

```sh
./mvnw verify
```

On Windows Command Prompt or PowerShell, use `mvnw.cmd verify`.

## Status

Early development. The project structure and CI are in place; functionality is built module by module, starting with the CRDT library.
