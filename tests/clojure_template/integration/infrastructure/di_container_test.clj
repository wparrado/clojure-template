(ns clojure-template.integration.infrastructure.di-container-test
    "Integration tests for the DI container — TrackingItemRepository and UoW."
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.infrastructure.di.container :as di]
              [clojure-template.infrastructure.persistence.in-memory.item-repository :as mem-repo]
              [clojure-template.infrastructure.events.in-process-publisher :as pub]
              [clojure-template.application.ports.unit-of-work :as uow-port]
              [clojure-template.domain.ports.outbound.item-repository :as repo-port]
              [clojure-template.domain.model.catalog.item :as item-agg])
    (:import [java.time Instant]))

(def ^:private now (Instant/now))

(defn- fresh-deps []
       (let [repo      (mem-repo/in-memory-item-repository)
             publisher (pub/in-process-publisher)]
            {:repo repo :publisher publisher :uow-factory (di/make-uow-factory repo publisher)}))

;; ── Factory containers ────────────────────────────────────────────────────────

(deftest make-persistence-container-returns-repo
         (testing "make-persistence-container returns map with :item-repository"
                  (let [c (di/make-persistence-container)]
                       (is (map? c))
                       (is (contains? c :item-repository)))))

(deftest make-events-container-returns-publisher
         (testing "make-events-container returns map with :event-publisher"
                  (let [c (di/make-events-container)]
                       (is (map? c))
                       (is (contains? c :event-publisher)))))

(deftest make-resilience-container-returns-circuit-breaker
         (testing "make-resilience-container returns map with :circuit-breaker"
                  (let [c (di/make-resilience-container)]
                       (is (map? c))
                       (is (contains? c :circuit-breaker)))))

;; ── UnitOfWork ────────────────────────────────────────────────────────────────

(deftest uow-factory-produces-fresh-uow
         (testing "uow-factory returns a new UoW on every call"
                  (let [{:keys [uow-factory]} (fresh-deps)
                        uow1 (uow-factory)
                        uow2 (uow-factory)]
                       (is (not (identical? uow1 uow2))))))

(deftest uow-get-item-repository-returns-tracking-repo
         (testing "get-item-repository on a fresh UoW returns a TrackingItemRepository"
                  (let [{:keys [uow-factory]} (fresh-deps)
                        uow  (uow-factory)
                        repo (uow-port/get-item-repository uow)]
                       (is (some? repo)))))

(deftest uow-commit-publishes-domain-events
         (testing "commit publishes events collected during the UoW scope"
                  (let [{:keys [uow-factory publisher]} (fresh-deps)
                        captured (atom [])
                        _        (pub/subscribe! publisher
                                                 :clojure-template.domain.model.catalog.item-events/item-created
                                                 #(swap! captured conj %))
                        uow      (uow-factory)
                        repo     (uow-port/get-item-repository uow)
                        new-item (item-agg/create-item {:name "Widget" :price 5.0 :now now})]
                       (repo-port/save repo new-item)
                       (uow-port/commit uow)
                       (is (= 1 (count @captured))))))

(deftest uow-rollback-discards-collected-events
         (testing "rollback discards events so they are NOT published on commit"
                  (let [{:keys [uow-factory publisher]} (fresh-deps)
                        captured (atom [])
                        _        (pub/subscribe! publisher
                                                 :clojure-template.domain.model.catalog.item-events/item-created
                                                 #(swap! captured conj %))
                        uow      (uow-factory)
                        repo     (uow-port/get-item-repository uow)
                        new-item (item-agg/create-item {:name "Widget" :price 5.0 :now now})]
                       (repo-port/save repo new-item)
                       (uow-port/rollback uow)
                       (is (empty? @captured)))))

;; ── TrackingItemRepository delegates ─────────────────────────────────────────

(deftest tracking-repo-find-by-id-delegates-to-store
         (testing "TrackingItemRepository.find-by-id delegates to the underlying store"
                  (let [{:keys [uow-factory]} (fresh-deps)
                        uow      (uow-factory)
                        repo     (uow-port/get-item-repository uow)
                        new-item (item-agg/create-item {:name "Widget" :price 5.0 :now now})
                        saved    (repo-port/save repo new-item)]
                       (is (some? (repo-port/find-by-id repo (:id saved)))))))

(deftest tracking-repo-find-all-delegates-to-store
         (testing "TrackingItemRepository.find-all delegates to the underlying store"
                  (let [{:keys [uow-factory]} (fresh-deps)
                        uow  (uow-factory)
                        repo (uow-port/get-item-repository uow)]
                       (repo-port/save repo (item-agg/create-item {:name "A" :price 1.0 :now now}))
                       (repo-port/save repo (item-agg/create-item {:name "B" :price 2.0 :now now}))
                       (let [{:keys [items total]} (repo-port/find-all repo 10 0)]
                            (is (= 2 total))
                            (is (= 2 (count items)))))))

(deftest tracking-repo-count-all-delegates-to-store
         (testing "TrackingItemRepository.count-all delegates to the underlying store"
                  (let [{:keys [uow-factory]} (fresh-deps)
                        uow  (uow-factory)
                        repo (uow-port/get-item-repository uow)]
                       (repo-port/save repo (item-agg/create-item {:name "X" :price 3.0 :now now}))
                       (is (= 1 (repo-port/count-all repo))))))

