# VeloRing: An Event-Driven Temporal Graph Engine for Real-Time Money Mule Ring Detection on Instant Payment Rails

## Project Title and Overview
**VeloRing** is a research-oriented system designed to address the challenge of detecting suspicious transaction-network patterns in fast, instant payment systems. As transaction speeds accelerate, bad actors exploit near-instant settlement times to orchestrate complex laundering maneuvers. VeloRing focuses on the streaming ingestion and active evaluation of temporal graph patterns over transaction histories, paving the way for real-time mule ring detection.

*Note: The detection algorithms themselves are planned for future phases. The current scope implements the robust foundational streaming, temporal, and historical infrastructure required to support them.*

## Problem Statement
Detecting modern financial crime involves several core challenges:
- **Rapid Fund Movement:** Bad actors move funds instantly through interconnected mule accounts.
- **Complex Topologies:** Mule topologies employ circular transaction flows, fan-out/fan-in patterns, and rapid pass-through behavior to obfuscate the ultimate destination of funds.
- **Temporal Blind Spots:** Analyzing transactions independently rather than evaluating their strict temporal relationships over short time windows significantly limits the ability to identify coordinated laundering rings.

## Research Objectives
- Build a reproducible synthetic payment stream to simulate laundering behavior.
- Ingest high-volume payment events through Kafka.
- Maintain event-time-based temporal state, avoiding wall-clock discrepancies.
- Represent transaction history safely and idempotently in a Neo4j graph database.
- Evaluate graph-pattern detection over temporal windows (in later phases).
- Measure performance empirically in the designated testing phase.

## System Architecture
The current architecture processes synthetic payment streams through a decoupled pipeline. The temporal window engine sits alongside the historical persistence layer, maintaining a highly responsive, in-memory view of the active state.

```mermaid
flowchart LR
    Gen[Synthetic Generator<br/>(Java)] -->|Kafka Topics| Broker(Kafka KRaft)
    Broker --> Ingest[Spring Boot Ingestion]
    Ingest -->|Idempotent Relationship Persistence| Graph[(Neo4j Graph Database)]
    Ingest -->|Event-Time State| Temporal{Temporal Window Engine}
    Temporal -.->|Immutable Snapshots| Phase5[Phase 5: Planned Detection Engine]
    Phase5 -.->|Alerts & Risks| Dashboard[Planned Dashboard Frontend]
```

### Components:
- **Implemented:** Synthetic Generator, Kafka Broker, Spring Boot Ingestion, Neo4j Historical Persistence, and the Temporal Window Engine.
- **Planned:** Graph Detection Engine, Risk Scoring, and a React-based Dashboard.

## Technology Stack
The stack has been strictly verified against the repository's configuration:
- **Java 21** and **Spring Boot 3.3.4** (Backend & Generator)
- **Apache Kafka 7.5.3** (running in KRaft mode)
- **Neo4j Community Edition 5.18.0** (with APOC plugin)
- **Maven** for build and dependency management
- **React, Vite, and TypeScript** (existing foundation for the planned frontend dashboard)
- **Docker Compose** for infrastructure orchestration

## Current Implementation Status
The project is being developed in strict phases.
- **Phase 1 — Foundation and infrastructure:** Completed.
- **Phase 2 — Synthetic payment generator:** Completed. Includes generation of synthetic transactions and scenario ground truth.
- **Phase 3 — Kafka ingestion hardening:** Completed. Features robust Kafka ingestion, event validation, retry handling, and idempotent Neo4j relationship persistence.
- **Phase 4 — Temporal window engine:** Completed. Fully implements event-time-based temporal state, watermark progression, out-of-order event handling, duplicate tracking within active state, synchronous eviction, and immutable snapshots.
- **Phase 5 — Circular mule detection:** Planned (Not Implemented).
- *Later detection, risk scoring, explainability, dashboard integrations, and load testing remain pending and planned for future iterations.*

## Temporal-Window Research Design
The temporal capabilities introduced in Phase 4 operate under explicit, reproducible rules:
- **Configuration:** `WINDOW_SIZE` defaults to 60 seconds. `ALLOWED_LATENESS` defaults to 60 seconds. These are conceptually independent and fully configurable.
- **Exclusion Boundaries:** The active window safely excludes events at or before the exact lower boundary (`(watermark - WINDOW_SIZE, watermark]`).
- **Determinism:** Event-time logic strictly utilizes transaction `eventTime` and is completely independent of wall-clock time (`System.currentTimeMillis()`), ensuring deterministic replayability.
- **Experimental Framing:** The 60-second window is an *experimental design parameter* used for testing temporal engine behaviors, not a universal or industry-standard Anti-Money Laundering (AML) rule.

## Repository Structure
```text
.
├── backend/            # Spring Boot application, Kafka consumer, and temporal engine
├── frontend/           # React, Vite, and TypeScript frontend
├── generator/          # Java-based synthetic payment generator
├── docker-compose.yml  # Kafka and Neo4j container orchestration
├── .env.example        # Environment variable template
└── .gitignore
```

## Prerequisites and Setup
### Prerequisites
- JDK 21
- Maven
- Docker and Docker Compose

### Setup Instructions
1. **Environment Configuration:**
   Copy the example environment template and configure secrets.
   ```bash
   cp .env.example .env
   ```
2. **Start Infrastructure (from the repository root):**
   ```bash
   docker-compose up -d
   ```
3. **Run Backend Tests:**
   ```bash
   cd backend
   mvn clean test
   ```

## Verification
The backend test suite acts as the primary validation mechanism for the infrastructure and temporal logic.
Run the tests using:
```bash
cd backend
mvn test
```
The backend currently supports **17 verified tests** that comprehensively enforce the temporal engine's strict watermark advancement, out-of-order latency evaluations, duplicate ejection, and Spring Boot context loading.

## Research Limitations and Responsible Interpretation
- **Synthetic Data:** Synthetic data is not proof of real-world fraud detection effectiveness.
- **Performance:** No unsupported precision, recall, throughput, or latency claims are currently made. Benchmarking belongs to a planned future phase.
- **Ground Truth:** Ground truth labels identify intentionally injected scenarios; they do not establish that every other generic transaction is legitimate.
- **Real-World Validation:** Real-world AML validation remains strictly as future work.

## Future Roadmap
- **Phase 5:** Implementation of circular mule ring detection algorithms.
- **Phase 6+:** Fan-out/fan-in and rapid pass-through topology detection.
- **Visualizations:** Connecting the processed snapshots to the frontend for real-time dashboard visualizations and alert explainability.

## Contribution and License
- **Contribution:** Guidelines have not yet been established.
- **License:** Licensing for this software has not yet been specified (no LICENSE file provided).
