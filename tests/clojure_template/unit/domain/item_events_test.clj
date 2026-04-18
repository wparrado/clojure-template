(ns clojure-template.unit.domain.item-events-test
    "Unit tests for Item domain events — IDomainEvent protocol."
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.domain.model.catalog.item-events :as events]
              [clojure-template.domain.events.base :as base])
    (:import [java.time Instant]))

(def ^:private now      (Instant/now))
(def ^:private agg-id   "00000000-0000-0000-0000-000000000001")

;; ── ItemCreated ───────────────────────────────────────────────────────────────

(deftest item-created-event-type
         (testing "ItemCreated.event-type returns ::item-created"
                  (let [evt (events/->ItemCreated agg-id now "Widget" 9.99 "desc")]
                       (is (= ::events/item-created (base/event-type evt))))))

(deftest item-created-aggregate-id
         (testing "ItemCreated.event-aggregate-id returns the aggregate id"
                  (let [evt (events/->ItemCreated agg-id now "Widget" 9.99 "desc")]
                       (is (= agg-id (base/event-aggregate-id evt))))))

(deftest item-created-occurred-at
         (testing "ItemCreated.event-occurred-at returns the instant"
                  (let [evt (events/->ItemCreated agg-id now "Widget" 9.99 "desc")]
                       (is (= now (base/event-occurred-at evt))))))

;; ── ItemUpdated ───────────────────────────────────────────────────────────────

(deftest item-updated-event-type
         (testing "ItemUpdated.event-type returns ::item-updated"
                  (let [evt (events/->ItemUpdated agg-id now "Gadget" 19.99 nil)]
                       (is (= ::events/item-updated (base/event-type evt))))))

(deftest item-updated-aggregate-id
         (testing "ItemUpdated.event-aggregate-id returns the aggregate id"
                  (let [evt (events/->ItemUpdated agg-id now "Gadget" 19.99 nil)]
                       (is (= agg-id (base/event-aggregate-id evt))))))

(deftest item-updated-occurred-at
         (testing "ItemUpdated.event-occurred-at returns the instant"
                  (let [evt (events/->ItemUpdated agg-id now "Gadget" 19.99 nil)]
                       (is (= now (base/event-occurred-at evt))))))

;; ── ItemDeleted ───────────────────────────────────────────────────────────────

(deftest item-deleted-event-type
         (testing "ItemDeleted.event-type returns ::item-deleted"
                  (let [evt (events/->ItemDeleted agg-id now)]
                       (is (= ::events/item-deleted (base/event-type evt))))))

(deftest item-deleted-aggregate-id
         (testing "ItemDeleted.event-aggregate-id returns the aggregate id"
                  (let [evt (events/->ItemDeleted agg-id now)]
                       (is (= agg-id (base/event-aggregate-id evt))))))

(deftest item-deleted-occurred-at
         (testing "ItemDeleted.event-occurred-at returns the instant"
                  (let [evt (events/->ItemDeleted agg-id now)]
                       (is (= now (base/event-occurred-at evt))))))
