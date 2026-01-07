# AI Trader Migration Blueprint

This document captures the current-state audit of the repository and a target architecture for migrating to a modern Java/PostgreSQL/Angular stack while preserving all existing functionality. It also serves as the authoritative log of migration tasks (planned, in-progress, and completed) so work can resume seamlessly after breaks.

## Migration Task Log (rolling)
| Task | Status | Notes |
| --- | --- | --- |
| Update migration plan with Angular-first UI, Liquibase adoption, and testing commitments | Completed | Locked in Angular and Liquibase; documented testing expectations. |
| Workflow recommendation | Completed | PR-based iteration with gated reviews approved. |
| Project structure proposal | Completed | Approved layout for backend + Angular UI, configs, and changelog locations. |
| Project scaffolding (backend + Angular) | Completed | Created Maven aggregator, Spring Boot skeleton with Liquibase placeholder, and Angular app scaffold. |
| Data layer implementation | Completed | Added entities, repositories, and Liquibase schema for core trading, audit, and ingestion tables. |
| Service & API layer (core CRUD) | Completed | Added baseline services/controllers, DTOs, validation, and API exception handling. |
| Containerization (Docker build/package/run) | Completed | Added backend/UI Dockerfiles, compose stack, and container run documentation. |
| API docs & observability | Completed | Swagger UI enabled via springdoc; actuator health/info exposed for container/compose runs. |
| CORS and UI integration readiness | Completed | Configurable CORS for /api added for Angular dev and containerized UI. |
| Security baseline (JWT auth & role protection) | Completed | Added stateless JWT auth with role-based API access and container env configuration. |
| Positions & portfolio snapshot APIs | Completed | Added services/controllers for positions and portfolio snapshots with stop-loss updates and filtering by account. |
| Upload ingestion APIs | Completed | Added upload metadata endpoints (create/list/get/status update) to track ingestion lifecycle. |
| Analytics summary API | Completed | Added account-level PnL/return/drawdown summary endpoint backed by portfolio snapshots. |
| Audit & trade log APIs | Completed | Added create/list/get endpoints for audit logs and trade logs to improve observability and reporting. |
| Quote snapshot API | Completed | Added quote snapshot endpoints (create/list/get with symbol filter) for market data ingestion/testing. |
| Market data adapter (snapshot-backed) | Completed | Added adapter/service/controller to expose latest quotes via stored quote snapshots. |

## Phase 1 — Deep Discovery

### Technology Stack Audit
- **Languages**: Python 3.11+ (scripts and CLIs).
- **Core libraries**: `pandas`, `numpy`, `yfinance`, `matplotlib`.
- **Optional libraries**: `pandas_datareader` (Stooq fallback), `requests` (helper downloads), `openai` (LLM automation).
- **Data/storage**: CSV files persisted alongside scripts (`chatgpt_portfolio_update.csv`, `chatgpt_trade_log.csv`); optional `tickers.json` for benchmark overrides.
- **Execution style**: CLI-first, interactive workflows; no background scheduler or web service.
- **Tests/CI**: None present.

### Operational Documentation
- **Setup**:
  1. Create and activate a Python virtual environment.
  2. Install dependencies: `pip install -r requirements.txt` (add `openai` and `pandas_datareader` if using automation/Stooq).
- **Primary workflows**:
  - **Portfolio processing** (`trading_script.py`):
    - Loads prior state from `chatgpt_portfolio_update.csv` (default in repo root; can be relocated via `set_data_dir` or `--file/--data-dir`).
    - Interactive prompt to log manual buys (market-on-open or limit) and sells; updates cash and holdings.
    - Persists trade logs to `chatgpt_trade_log.csv` and portfolio snapshots to the portfolio CSV.
    - Outputs daily metrics (price/volume per holding and benchmarks, equity, drawdown, CAPM stats).
    - Env override: `ASOF_DATE=YYYY-MM-DD` to backdate runs for backtesting.
  - **Starter templates** (`Start Your Own/ProcessPortfolio.py`):
    - Thin wrapper that runs `trading_script.main` using the `Start Your Own` data directory.
  - **Performance charting** (`Start Your Own/Generate_Graph.py`):
    - Reads TOTAL rows from the portfolio CSV, downloads S&P 500 via `yfinance`, normalizes to a baseline, and plots/exports a comparison chart.
  - **Automation** (`simple_automation.py` + `AUTOMATION_README.md`):
    - Builds an LLM prompt from the latest portfolio, calls OpenAI (`OPENAI_API_KEY` or `--api-key`), parses JSON suggestions, and (simulated) executes trades; logs responses to `llm_responses.jsonl`. Uses the same CSV storage as the manual workflow.
- **Artifacts/inputs**:
  - Portfolio CSV columns include per-ticker rows plus `TOTAL` summary rows (equity, cash).
  - Trade log captures manual buys/sells with reasons and execution prices.
  - Benchmarks default to `["IWO", "XBI", "SPY", "IWM"]` unless overridden by `tickers.json`.

### Architectural Mapping (Current)
- **Data flow**:
  1. User (or LLM) inputs trades via CLI.
  2. Script fetches market data with Yahoo Finance, falling back to Stooq (via `pandas_datareader` or CSV) and proxy symbols.
  3. Portfolio state and trade logs are written to CSV files.
  4. Reporting reads the CSV history to compute equity curve, drawdowns, and CAPM-like metrics; charting script downloads S&P 500 for benchmarking.
- **Component interactions**:
  - `trading_script.py` owns data access, trade logging, metrics, and persistence.
  - `simple_automation.py` composes `trading_script` helpers to generate prompts and simulate execution.
  - `Start Your Own` scripts wrap the same core module with a local data directory.
- **State management**: Flat-file (CSV) persistence only; no database. Global paths for portfolio/trade logs can be rebased via CLI options or `set_data_dir`. No server-side session/state.
- **Observability**: Console output and CSV/JSONL logs; no centralized logging, metrics, or alerting.

## Phase 2 — Target Architecture & Migration Design

### Platform Recommendation
- **Backend choice**: Prefer **Spring Boot 3 / Java 21** for the broader ecosystem (Spring Data JPA, Spring Batch, scheduling, actuator observability, and mature finance/CSV tooling). Quarkus offers faster cold start and lower footprint; use it only if ultra-low-latency container startup is a priority over ecosystem depth.
- **Persistence**: PostgreSQL via Spring Data JPA + Hibernate.
- **Build/packaging**: Maven (multi-module project) with Docker images per service; docker-compose for local dev and optional Kubernetes manifests/Helm for staging/prod.
- **Frontend**: **Angular** (settled choice for the initial web UI) with a component library (e.g., Angular Material) and charting via Recharts-like options for Angular (ngx-charts/D3/Highcharts). React can remain an alternative later, but Angular is the default for this migration.

### Target System Design
- **Proposed modules** (Spring Boot services; can be mono-repo with Maven modules initially):
  - **Gateway/API**: REST layer (Spring Web) with DTOs, validation, and OpenAPI docs.
  - **Trading & Portfolio Service**: Manages holdings, cash, order lifecycle, stop-loss logic, and equity curve computation; exposes endpoints for manual and automated orders.
  - **Market Data Adapter**: Pluggable connectors (Yahoo, Stooq, broker/exchange APIs). Use Spring’s `@Async` or Reactor for concurrent fetches; cache intraday quotes (Redis optional).
  - **Ingestion Service**: File upload endpoints (CSV/JSON/Excel) using Spring MVC + Apache POI; validates and stages data before committing to core tables.
  - **Reporting/Analytics**: Precomputes and serves time-series (equity, drawdown, CAPM stats) and generates chart-ready aggregates.
  - **Audit & Logging**: Central audit trail persisted in PostgreSQL (orders, state transitions, user actions, LLM decisions); emit structured logs to stdout/OTel.
  - **Scheduler**: Spring Scheduler (or Batch) for daily calculations, reconciliations, and backfills.
- **Data model (initial cut)**:
  - `users` (id, email, role, api_key_hash/oidc_sub)
  - `accounts` (id, user_id, base_currency, cash_balance)
  - `positions` (id, account_id, symbol, qty, cost_basis, stop_loss, opened_at, closed_at)
  - `orders` (id, account_id, symbol, side, type, limit_price, stop_price, qty, status, routed_at, filled_at, source=manual|automation)
  - `executions` (id, order_id, price, qty, venue, timestamp)
  - `quotes`/`prices` (symbol, ts, open/high/low/close/volume, source)
  - `portfolio_snapshots` (id, account_id, date, equity, cash, pnl, drawdown)
  - `trade_logs` (id, account_id, action, reason, metadata JSONB)
  - `uploads` (id, user_id, type, status, blob_location, parsed_row_count, error_report)
  - `audit_logs` (id, actor, action, entity_ref, before/after JSONB, ts)
  - `benchmarks` (id, symbol, name)
- **API sketch**:
  - `/api/orders` (POST buy/sell/stop/limit; GET status/history)
  - `/api/positions` (GET current; DELETE/POST stop updates)
  - `/api/uploads` (POST multipart CSV/Excel/JSON; GET status/errors)
  - `/api/portfolio/snapshots` (GET time-series; supports as-of/backtest params)
  - `/api/analytics/summary` (GET equity/drawdown/CAPM metrics)
  - `/api/benchmarks` (CRUD for benchmark sets)
  - `/api/audit` (GET filtered audit trail)
- **Frontend (Angular)**:
  - Angular app with routing for Dashboard, Orders, Positions, Uploads, Analytics, Audit Log, Config.
  - Charts via ngx-charts/D3/Highcharts; data grids with server-side pagination; file upload widget with validation preview; Angular Material components.
- **Cross-cutting concerns**:
  - AuthN/Z: JWT or OIDC; role-based access for trading vs. read-only.
  - Validation: Bean Validation on DTOs; schema validation for uploads.
  - Observability: Spring Boot Actuator, Prometheus metrics, centralized logging.
  - Testing: JUnit 5 + Testcontainers for PostgreSQL; contract tests for adapters.

### Migration Approach (High-Level)
- Start as a **modular monolith** (API + ingestion + trading + analytics in one Spring Boot app) with clear package boundaries; extract services later if needed.
- Define canonical domain models that mirror current CSV schemas to ease data import.
- Build importers to transform existing `chatgpt_portfolio_update.csv` and `chatgpt_trade_log.csv` into database seeds.
- Implement adapters for Yahoo/Stooq first to maintain parity, then abstract for broker/exchange connectivity.

### Testing Commitments (per component)
- **Unit tests**: JUnit 5 for Java backend components; Jasmine/Karma (or Jest) for Angular components.
- **Integration tests**: Testcontainers-powered PostgreSQL for repositories and service flows; REST contract tests for APIs.
- **Logical/functional tests**: End-to-end scenarios covering order lifecycle, ingestion, analytics, and UI flows (Playwright/Cypress for Angular).
- **Coverage goal**: Define and enforce minimum coverage thresholds per module once code standards are confirmed.

### Database Migration Strategy
- **Migration tool**: Liquibase will manage all schema changes and reference data loads.
- **Practices**: Versioned changelogs per module, idempotent changesets, rollback definitions where feasible, and automated validation in CI prior to deployment.

## Phase 3 — Functional Parity & Enhancements

### Parity Requirements
- Preserve manual trade entry (market-on-open, limit) and stop-loss handling.
- Maintain benchmark comparisons (default symbols configurable like current `tickers.json` behavior).
- Retain portfolio/equity calculations, drawdown metrics, and daily reporting.
- Support backdated/as-of processing for backtests similar to `ASOF_DATE`.
- Keep logging of every trade/action with reasons, akin to current CSV logs.

### New Capabilities (Design Outline)
1) **Unified Command & Control UI**
   - Dashboard showing cash, equity curve, open positions, recent orders, and benchmarks.
   - Order ticket supporting manual/automated triggers, stop/limit entry, and validation against cash/limits.
   - Admin screens for configuration (benchmarks, risk params) with audit hooks.

2) **Data Ingestion & Persistence**
   - File upload endpoints for CSV/JSON/Excel; server-side schema validation, row-level error reporting, and staged imports before commit.
   - PostgreSQL-backed ledger capturing orders, executions, snapshots, and audits; nightly snapshots for continuity.

3) **Analytics & Integration**
   - Reporting suite delivering equity/drawdown/CAPM, per-symbol PnL, and benchmark overlays via REST and charts.
   - Adapter pattern for external data providers/exchanges/news; interface contracts + per-provider modules to swap sources without touching core logic.
   - Optional LLM integration service mirroring `simple_automation.py` behavior with stored prompts/responses and guardrails.

### Delivery Roadmap (detailed tasks)
1. **Foundations**
   - Initialize Maven multi-module repo (core, api, ingestion, analytics, ui-angular).
   - Configure Java 21 toolchain, code style, Spotless/Checkstyle, and baseline CI.
   - Add Dockerfiles and docker-compose for app + PostgreSQL; seed `.env` templates.
2. **Data Model & Migration**
   - Define Liquibase changelogs for schema (users, accounts, positions, orders, executions, quotes, portfolio_snapshots, trade_logs, uploads, audit_logs, benchmarks).
   - Implement JPA entities, repositories, and value objects mirroring CSV semantics.
   - Build CSV importers to ingest `chatgpt_portfolio_update.csv` and `chatgpt_trade_log.csv` into the database; add parity checks against Python outputs.
3. **Market Data Adapter Layer**
   - Implement Yahoo/Stooq connectors (with proxy map) and caching; expose quote/ohlcv services.
   - Add health checks and source telemetry.
4. **Trading & Portfolio Service**
   - Implement domain services for orders (market-on-open, limit, stop), stop-loss updates, position lifecycle, and cash/equity computation.
   - Add CAPM/drawdown metrics aligned with current Python outputs; provide as-of/backtest handling.
5. **API Gateway**
   - Expose REST endpoints for orders, positions, benchmarks, portfolio snapshots, analytics summary, uploads, and audit log queries.
   - Add request/response DTOs, validation, and OpenAPI documentation.
   - Implement authentication/authorization (JWT/OIDC) with role-based access.
6. **Ingestion Service**
   - Implement multipart upload endpoints for CSV/JSON/Excel with schema validation and row-level error reporting.
   - Stage data prior to commit; surface processing status via APIs.
7. **Analytics & Reporting**
   - Provide chart-ready time-series (equity, drawdown, CAPM stats) and per-symbol PnL endpoints.
   - Add scheduled jobs for nightly snapshots and reconciliations.
8. **Audit & Observability**
   - Persist audit trail for orders, configuration changes, and LLM-driven actions.
   - Enable Spring Boot Actuator, structured logging, and Prometheus/OpenTelemetry hooks.
9. **Frontend (Angular)**
   - Scaffold Angular app with routing: Dashboard, Orders, Positions, Uploads, Analytics, Audit Log, Config.
   - Integrate Angular Material components and charting (ngx-charts/D3/Highcharts).
   - Implement order ticket (manual/automated), data tables with server-side pagination, upload workflow with validation preview, and analytics charts.
   - Add auth flows (login, token refresh) and environment configs for API URLs.
10. **Automation & LLM Integration (optional phase)**
    - Port `simple_automation.py` behavior into a service that stores prompts/responses and applies guardrails.
    - Provide UI controls to trigger and review automated suggestions.
11. **Testing & Hardening**
    - Add JUnit 5 + Testcontainers for PostgreSQL/adapters; contract tests for APIs; frontend e2e tests (Cypress/Playwright).
    - Load testing for quote and order endpoints; security scans; rollout scripts for staging/prod.

This plan keeps current behaviors (manual trading, benchmark comparisons, backdated runs) while introducing a scalable, observable, and extensible Java/PostgreSQL/Angular platform.
