(ns clojure-template.infrastructure.di.container
    "Sub-containers wiring adapters to ports."
    (:require [clojure-template.infrastructure.persistence.in-memory.item-repository :as mem-repo]
              [clojure-template.infrastructure.events.in-process-publisher :as publisher]
              [clojure-template.infrastructure.resilience.circuit-breaker :as cb]
              [clojure-template.domain.ports.outbound.item-repository :as repo-port]
              [clojure-template.domain.ports.outbound.event-publisher :as pub-port]
              [clojure-template.domain.model.aggregate :as agg]
              [clojure-template.application.ports.unit-of-work :as uow-port]))

;; ── TrackingItemRepository ────────────────────────────────────────────────────
;;
;; Decorator that intercepts save/delete calls, collects domain events from
;; saved aggregates into a shared atom, and delegates storage to the real repo.

(defrecord TrackingItemRepository [delegate collected-events]
  repo-port/IItemRepository
  (save [_ item]
    (let [[clean-item evts] (agg/collect-events item)]
         (swap! collected-events into evts)
         (repo-port/save delegate clean-item)))
  (find-by-id [_ id]        (repo-port/find-by-id delegate id))
  (find-all   [_ limit offset] (repo-port/find-all delegate limit offset))
  (find-matching [_ spec limit offset] (repo-port/find-matching delegate spec limit offset))
  (count-all  [_]           (repo-port/count-all delegate)))

;; ── UnitOfWork ────────────────────────────────────────────────────────────────

(defrecord InMemoryUnitOfWork [tracking-repo publisher collected-events]
  uow-port/IUnitOfWork
  (get-item-repository [_] tracking-repo)
  (commit [_]
    (let [evts @collected-events]
         (pub-port/publish-all publisher evts)
         (reset! collected-events [])))
  (rollback [_]
    ;; NOTE: The in-memory delegate does not support transactional undo.
    ;; Writes already delegated to the store are NOT reversed on rollback.
    ;; Only collected domain events are discarded, preventing stale event
    ;; publication. Replace with a real transactional adapter for production use.
    (reset! collected-events [])))

(defn make-uow-factory
      "Returns a zero-arg factory that produces a fresh InMemoryUnitOfWork backed
   by a TrackingItemRepository that collects domain events for publication."
      [repo pub]
      (fn []
          (let [evts     (atom [])
                tracking (->TrackingItemRepository repo evts)]
               (->InMemoryUnitOfWork tracking pub evts))))

;; ── Persistence container ─────────────────────────────────────────────────────

(defn make-persistence-container []
      {:item-repository (mem-repo/in-memory-item-repository)})

;; ── Events container ──────────────────────────────────────────────────────────

(defn make-events-container []
      {:event-publisher (publisher/in-process-publisher)})

;; ── Resilience container ──────────────────────────────────────────────────────

(defn make-resilience-container []
      {:circuit-breaker (cb/simple-circuit-breaker)})
