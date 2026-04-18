# clojure-template

A production-ready Clojure project template implementing **Hexagonal Architecture (Ports & Adapters)** over an Item/Catalog domain.

It bundles a complete quality pipeline: static analysis, auto-formatting, a three-tier test pyramid (unit → integration → architecture), line-coverage enforcement, and CVE scanning — all wired into a pre-commit hook and a GitHub Actions CI workflow.

---

## Table of Contents

- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Endpoints](#api-endpoints)
- [Testing](#testing)
- [Quality Gates](#quality-gates)
- [Git Hooks](#git-hooks)
- [CI](#ci)
- [Adding a New Use Case](#adding-a-new-use-case)
- [Adding a New Domain Entity](#adding-a-new-domain-entity)

---

## Architecture

The project is organized in four strictly isolated layers. Each layer may only depend on layers below it; the architecture tests enforce this at every commit.

```
┌─────────────────────────────────────────────────────────────┐
│  Presentation  (HTTP / Ring / Reitit)                       │
│  Schemas · Routers · Middlewares · Error handlers           │
├─────────────────────────────────────────────────────────────┤
│  Application  (Use cases / CQRS)                            │
│  Command handlers · Query handlers · Result type · DTOs     │
│  Ports (inbound & outbound interfaces)                      │
├─────────────────────────────────────────────────────────────┤
│  Domain  (Pure business logic — zero external deps)         │
│  Aggregates · Value Objects · Domain Events · Specs · Ports │
├─────────────────────────────────────────────────────────────┤
│  Infrastructure  (Adapters)                                 │
│  In-memory repository · In-process event publisher         │
│  System clock · Circuit breaker · DI container             │
└─────────────────────────────────────────────────────────────┘
```

Key design decisions:

| Decision | Details |
|---|---|
| **CQRS** | Commands (`Create`, `Update`, `Delete`) and queries (`Get`, `List`, `Search`) are handled by separate handler records. |
| **Railway-oriented programming** | All use-case results flow through `Result<T>` (`ok`/`err`). Handlers never throw directly into the HTTP layer. |
| **Unit of Work** | Command handlers obtain a fresh `IUnitOfWork` per invocation; the UoW collects domain events and publishes them on `commit`. |
| **Domain Events** | Aggregates accumulate events in an internal vector. The `TrackingItemRepository` decorator drains and collects them before persisting. |
| **Specifications** | Composable `ISpecification` predicates (`and-spec`, `or-spec`, `not-spec`) drive filtered queries without leaking business rules into the repository. |

---

## Project Structure

```
.
├── src/clojure_template/
│   ├── main.clj                          # Entry point — builds Ring app, starts Jetty
│   ├── app_container.clj                 # Composition root — wires all adapters
│   ├── settings.clj                      # Config loader (aero + env vars)
│   │
│   ├── domain/
│   │   ├── model/
│   │   │   ├── entity.clj                # IEntity protocol
│   │   │   ├── aggregate.clj             # IAggregateRoot protocol + collect-events
│   │   │   ├── value_object.clj          # IValueObject protocol
│   │   │   └── catalog/
│   │   │       ├── item.clj              # Item aggregate root (create/update/delete)
│   │   │       ├── value_objects.clj     # ItemId · ItemName · ItemDescription · ItemPrice
│   │   │       └── item_events.clj       # ItemCreated · ItemUpdated · ItemDeleted
│   │   ├── events/base.clj               # IDomainEvent protocol
│   │   ├── exceptions/domain_errors.clj  # ex-info error constructors + predicates
│   │   ├── ports/outbound/
│   │   │   ├── item_repository.clj       # IItemRepository port
│   │   │   └── event_publisher.clj       # IDomainEventPublisher port
│   │   ├── ports/inbound/clock.clj       # IClock port
│   │   └── specifications/
│   │       ├── base.clj                  # ISpecification + and/or/not combinators
│   │       └── item_specifications.clj   # ActiveItemSpec · NameContainsSpec · PriceRangeSpec
│   │
│   ├── application/
│   │   ├── result.clj                    # Result<T>: ok/err/map-ok/flat-map/try-result/with-uow
│   │   ├── commands/item_commands.clj    # CreateItemCommand · UpdateItemCommand · DeleteItemCommand
│   │   ├── queries/item_queries.clj      # GetItemQuery · ListItemsQuery · SearchItemsQuery
│   │   ├── handlers/
│   │   │   ├── command_handlers.clj      # CreateItemHandler · UpdateItemHandler · DeleteItemHandler
│   │   │   └── query_handlers.clj        # GetItemHandler · ListItemsHandler · SearchItemsHandler
│   │   ├── services/item_service.clj     # ItemApplicationService (implements IItemApplicationService)
│   │   ├── mappers/item_mapper.clj       # Item aggregate → ItemDto
│   │   ├── dtos/
│   │   │   ├── item_dtos.clj             # ItemDto record
│   │   │   └── pagination.clj            # PaginatedResult + paginate helper
│   │   ├── exceptions.clj                # Re-exports domain error constructors for app layer
│   │   └── ports/
│   │       ├── item_application_service.clj  # IItemApplicationService (inbound)
│   │       ├── unit_of_work.clj              # IUnitOfWork port
│   │       └── circuit_breaker.clj           # ICircuitBreaker port
│   │
│   ├── infrastructure/
│   │   ├── clock/system_clock.clj        # IClock → System.currentTimeMillis
│   │   ├── events/in_process_publisher.clj  # IDomainEventPublisher (sync, in-process)
│   │   ├── persistence/in_memory/
│   │   │   └── item_repository.clj       # IItemRepository backed by an atom
│   │   ├── resilience/circuit_breaker.clj   # ICircuitBreaker (closed/open/half-open FSM)
│   │   └── di/container.clj              # TrackingItemRepository · InMemoryUnitOfWork · sub-containers
│   │
│   └── presentation/
│       ├── error_handlers.clj            # Domain error → HTTP status + JSON body
│       ├── middlewares/correlation_id.clj  # Injects X-Correlation-ID header
│       ├── mappers/item_schema_mapper.clj  # HTTP body ↔ commands/queries/responses
│       └── api/v1/
│           ├── schemas/item_schemas.clj  # Malli schemas for request validation
│           └── routers/item_router.clj   # Reitit routes for /api/v1/items
│
├── tests/clojure_template/
│   ├── unit/
│   │   ├── domain/                       # Item aggregate, value objects, specs, domain errors
│   │   ├── application/                  # Result type, mappers, pagination, handlers (pure)
│   │   └── presentation/                 # Error handlers, middleware, schema mapper, schemas, router
│   ├── integration/
│   │   ├── application/                  # Command/query handlers with in-memory adapters
│   │   └── infrastructure/               # IItemRepository contract tests
│   └── architecture/
│       └── dependencies_test.clj         # Layer boundary enforcement
│
├── resources/config.edn                  # Aero config (reads env vars with defaults)
├── deps.edn                              # Dependencies + aliases
├── tests.edn                             # Kaocha test suites
├── cljfmt.edn                            # Formatting rules
├── nvd-config.edn                        # CVE scan settings
└── .githooks/pre-commit                  # format-check + lint + unit + architecture
```

---

## Tech Stack

| Concern | Library |
|---|---|
| HTTP server | `ring/ring-jetty-adapter` |
| Routing | `metosin/reitit` |
| Schema validation & coercion | `metosin/malli` |
| Content negotiation (JSON, Transit, EDN) | `metosin/muuntaja` |
| Configuration | `aero/aero` |
| Logging | `com.taoensso/timbre` |
| Test runner | `lambdaisland/kaocha` |
| Property-based testing | `org.clojure/test.check` |
| Coverage | `cloverage/cloverage` |
| Static analysis | `clj-kondo/clj-kondo` |
| Formatting | `dev.weavejester/cljfmt` |
| CVE scanning | `nvd-clojure/nvd-clojure` |

---

## Prerequisites

- **Java 21+** (tested with Temurin)
- **Clojure CLI** (`clj`) — [install guide](https://clojure.org/guides/install_clojure)

---

## Configuration

All configuration is read from `resources/config.edn` via [aero](https://github.com/juxt/aero), which resolves `#env` tags against environment variables with fallback defaults.

| Env var | Default | Description |
|---|---|---|
| `APP_PORT` | `3000` | HTTP port |
| `APP_ENV` | `development` | Environment name |
| `LOG_LEVEL` | `info` | Timbre log level |

Copy `.env.example` and adjust as needed:

```bash
cp .env.example .env
# edit .env, then source it or use direnv
```

---

## Running the Application

```bash
clj -M:run
```

The server starts on `http://localhost:3000` (or the port set in `APP_PORT`).

---

## API Endpoints

Base path: `/api/v1/items`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/items` | List items (paginated). Query params: `limit` (default 20, max 100), `offset` (default 0). |
| `POST` | `/api/v1/items` | Create an item. Body: `{name, description?, price}`. |
| `GET` | `/api/v1/items/:id` | Get a single item by UUID. |
| `PUT` | `/api/v1/items/:id` | Partially update an item. Body fields are all optional. |
| `DELETE` | `/api/v1/items/:id` | Soft-delete an item. Returns 204. |
| `GET` | `/api/v1/items/search` | Search items. Query params: `name-contains?`, `min-price?`, `max-price?`, `limit`, `offset`. |

All responses are JSON. Every response includes an `X-Correlation-ID` header.

**Error shape:**
```json
{
  "error": "Item not found",
  "type": "not-found",
  "details": { "id": "..." }
}
```

| HTTP status | Domain error |
|---|---|
| 404 | Not found |
| 409 | Conflict |
| 422 | Validation error |
| 500 | Unexpected error |

---

## Testing

Tests are organized in three suites. Run all or focus on one:

```bash
clj -M:test                          # all suites
clj -M:test --focus :unit            # fast, isolated — domain, application, presentation
clj -M:test --focus :integration     # full flows with in-memory adapters
clj -M:test --focus :architecture    # layer boundary enforcement
```

### Test pyramid

```
              Unit (61 tests)
        ┌──────────────────────────┐
        │ domain/       9 tests    │  Item aggregate, value objects, specs, domain errors
        │ application/ 19 tests    │  Result<T>, mappers, pagination
        │ presentation/26 tests    │  Error handlers, middleware, schema mapper, Malli schemas, router
        └──────────────────────────┘
              Integration (16 tests)
        ┌──────────────────────────┐
        │ application/ 15 tests    │  Command + query handlers with real in-memory adapters
        │ infrastructure/ 1 suite  │  IItemRepository contract (5 assertions)
        └──────────────────────────┘
              Architecture (6 tests)
        ┌──────────────────────────┐
        │ Layer boundary rules     │  domain ↛ app/infra/presentation                    │
        │                          │  application ↛ infra                                 │
        │                          │  presentation ↛ domain (direct)                      │
        │                          │  infrastructure ↛ presentation                        │
        └──────────────────────────┘
```

**Total: 108 tests, 261 assertions.**

### Coverage

```bash
clj -M:coverage    # enforces minimum 90% line coverage
```

The HTML report is written to `target/coverage/`.

---

## Quality Gates

```bash
clj -M:format-check   # verify formatting (cljfmt)
clj -M:format         # auto-fix formatting
clj -M:lint           # static analysis (clj-kondo)
clj -M:coverage       # run tests + enforce 90% line coverage
```

**CVE scan** — requires a free [NVD API key](https://nvd.nist.gov/developers/request-an-api-key). Export it and run:

```bash
export NVD_API_KEY=your-key-here
clojure -J-Dclojure.main.report=stderr -M:nvd "nvd-config.edn" "$(clojure -Spath)"
```

> The scan fails the build on any dependency with CVSS score ≥ 7 (HIGH or CRITICAL).

---

## Git Hooks

Hooks live in `.githooks/` and are version-controlled. Activate them once after cloning:

```bash
git config core.hooksPath .githooks
```

The **pre-commit hook** runs automatically before every commit:

| Step | Command |
|---|---|
| Format check | `clj -M:format-check` |
| Lint | `clj -M:lint` |
| Unit + architecture tests | `clj -M:test --focus :unit --focus :architecture` |

Integration tests and CVE scan are skipped locally — they run only in CI to keep commits fast.

---

## CI

GitHub Actions runs on every push and pull request to `main`.

| Step | What it enforces |
|---|---|
| Format check | Zero formatting drift |
| Lint | No unused vars, no forbidden imports, idiomatic code |
| Unit tests | Domain + application + presentation layers in isolation |
| Integration tests | Full flows with in-memory adapters |
| Architecture tests | Layer boundary violations fail the build |
| Coverage | Minimum **90%** line coverage (cloverage) |
| CVE scan | OWASP NVD — fails on CVSS ≥ 7 (HIGH/CRITICAL). Skipped if `NVD_API_KEY` secret is not set. |

---

## Adding a New Use Case

A **use case** is a new command or query on the existing `Item` domain. Example: adding a `ActivateItemCommand`.

### 1. Add the command (or query)

```clojure
;; src/clojure_template/application/commands/item_commands.clj
(defn ->ActivateItemCommand [id]
  {:type :activate-item :id id})
```

### 2. Add domain behavior on the aggregate

```clojure
;; src/clojure_template/domain/model/catalog/item.clj
(defn activate-item [item {:keys [now]}]
  (when (:deleted item)
    (throw (errors/not-found-error "Cannot activate a deleted item" {:id (:id item)})))
  (-> item
      (assoc :active true :updated-at now)
      (update :events conj (events/->ItemActivated (:id item) now))))
```

### 3. Add the handler

```clojure
;; src/clojure_template/application/handlers/command_handlers.clj
(defrecord ActivateItemHandler [uow-factory clock]
  ICommandHandler
  (handle [_ cmd]
    (result/try-result
     (result/with-uow [uow (uow-factory)]
       (let [repo     (uow-port/get-item-repository uow)
             now      (clock-port/now clock)
             existing (repo-port/find-by-id repo (:id cmd))]
         (when (nil? existing)
           (throw (errors/not-found-error "Item not found" {:id (:id cmd)})))
         (mapper/item->dto (repo-port/save repo (item-agg/activate-item existing {:now now}))))))))
```

### 4. Expose it on the application service port and implementation

```clojure
;; application/ports/item_application_service.clj — add to IItemApplicationService:
(activate-item [this cmd] "Handles ActivateItemCommand. Returns Result<ItemDto>.")

;; application/services/item_service.clj — add to ItemApplicationService:
(activate-item [_ cmd] (cmds/handle activate-handler cmd))
```

### 5. Wire it in the composition root

```clojure
;; app_container.clj
(activate-h (cmds/->ActivateItemHandler uow-factory clock))
;; pass activate-h to ->ItemApplicationService
```

### 6. Add the HTTP route

```clojure
;; presentation/api/v1/routers/item_router.clj
(defn activate-item-handler [svc]
  (fn [request]
    (let [id  (get-in request [:parameters :path :id])
          cmd (item-cmds/->ActivateItemCommand id)
          res (svc-port/activate-item svc cmd)]
      (result->response res #(ok-response (mapper/dto->response %))))))

;; Add to item-routes:
["/:id/activate" {:post {:handler    (activate-item-handler svc)
                          :parameters {:path {:id schemas/ItemIdParam}}}}]
```

### 7. Write the tests

- **Unit test** in `tests/.../unit/domain/item_test.clj` — test `activate-item` behavior in isolation.
- **Integration test** in `tests/.../integration/application/` — test the full handler flow.
- **Presentation test** in `tests/.../unit/presentation/item_router_test.clj` — test the HTTP handler with a stub service.

---

## Adding a New Domain Entity

To model a completely new aggregate (e.g., `Category`), follow this checklist:

1. **Domain layer**
   - `domain/model/catalog/category.clj` — `defrecord Category`, factory `create-category`, mutation functions
   - `domain/model/catalog/category_events.clj` — `CategoryCreated`, etc.
   - `domain/ports/outbound/category_repository.clj` — `ICategoryRepository` protocol

2. **Application layer**
   - `application/commands/category_commands.clj` — command maps
   - `application/queries/category_queries.clj` — query maps
   - `application/handlers/category_command_handlers.clj` — handler records
   - `application/handlers/category_query_handlers.clj`
   - `application/dtos/category_dtos.clj` — `CategoryDto`
   - `application/mappers/category_mapper.clj` — aggregate → DTO
   - `application/ports/category_application_service.clj` — `ICategoryApplicationService`
   - `application/services/category_service.clj` — implementation

3. **Infrastructure layer**
   - `infrastructure/persistence/in_memory/category_repository.clj` — in-memory adapter
   - Wire a new `ICategoryRepository` into the UoW in `di/container.clj`

4. **Presentation layer**
   - `presentation/api/v1/schemas/category_schemas.clj` — Malli schemas
   - `presentation/mappers/category_schema_mapper.clj`
   - `presentation/api/v1/routers/category_router.clj` — reitit routes
   - Register the new router in `main.clj`

5. **Tests** — mirror the same structure in `tests/`, one file per layer.

6. **Architecture tests** — the existing `dependencies_test.clj` will automatically enforce boundaries for the new namespaces with no changes needed.

