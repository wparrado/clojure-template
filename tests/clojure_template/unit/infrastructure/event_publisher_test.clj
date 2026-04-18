(ns clojure-template.unit.infrastructure.event-publisher-test
    "Unit tests for InProcessEventPublisher."
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.infrastructure.events.in-process-publisher :as pub]
              [clojure-template.domain.ports.outbound.event-publisher :as port]
              [clojure-template.domain.events.base :as base])
    (:import [java.time Instant]))

(defrecord StubEvent [etype agg-id]
  base/IDomainEvent
  (event-type         [_] etype)
  (event-aggregate-id [_] agg-id)
  (event-occurred-at  [_] (Instant/now)))

;; ── Constructor ───────────────────────────────────────────────────────────────

(deftest no-arg-constructor-creates-empty-publisher
         (testing "zero-arg constructor publishes events but no handler is called"
                  (let [publisher (pub/in-process-publisher)
                        called?   (atom false)]
                       (port/publish publisher (->StubEvent ::any-event "agg-1"))
                       (is (not @called?)))))

(deftest handlers-map-constructor-pre-registers-handlers
         (testing "handlers-map constructor invokes pre-registered handlers on publish"
                  (let [captured  (atom [])
                        publisher (pub/in-process-publisher {::test-event [(fn [e] (swap! captured conj e))]})]
                       (port/publish publisher (->StubEvent ::test-event "agg-1"))
                       (is (= 1 (count @captured))))))

;; ── subscribe! ────────────────────────────────────────────────────────────────

(deftest subscribe-registers-handler
         (testing "subscribe! registers a handler that is invoked on publish"
                  (let [publisher (pub/in-process-publisher)
                        captured  (atom [])]
                       (pub/subscribe! publisher ::my-event #(swap! captured conj %))
                       (port/publish publisher (->StubEvent ::my-event "agg-1"))
                       (is (= 1 (count @captured))))))

(deftest subscribe-multiple-handlers-for-same-type
         (testing "multiple handlers can be subscribed to the same event type"
                  (let [publisher (pub/in-process-publisher)
                        results   (atom #{})]
                       (pub/subscribe! publisher ::ev (fn [_] (swap! results conj :h1)))
                       (pub/subscribe! publisher ::ev (fn [_] (swap! results conj :h2)))
                       (port/publish publisher (->StubEvent ::ev "agg-1"))
                       (is (= #{:h1 :h2} @results)))))

;; ── publish ───────────────────────────────────────────────────────────────────

(deftest publish-passes-event-to-handler
         (testing "publish passes the event to the registered handler"
                  (let [publisher (pub/in-process-publisher)
                        received  (atom nil)]
                       (pub/subscribe! publisher ::ev #(reset! received %))
                       (let [evt (->StubEvent ::ev "agg-42")]
                            (port/publish publisher evt)
                            (is (= evt @received))))))

(deftest publish-ignores-unknown-event-types
         (testing "publish does nothing when no handler is registered for the type"
                  (let [publisher (pub/in-process-publisher)
                        called?   (atom false)]
                       (pub/subscribe! publisher ::other-event (fn [_] (reset! called? true)))
                       (port/publish publisher (->StubEvent ::unknown-event "agg-1"))
                       (is (not @called?)))))

;; ── publish-all ───────────────────────────────────────────────────────────────

(deftest publish-all-invokes-handler-per-event
         (testing "publish-all invokes handler once per event"
                  (let [publisher (pub/in-process-publisher)
                        captured  (atom [])]
                       (pub/subscribe! publisher ::ev #(swap! captured conj %))
                       (port/publish-all publisher [(->StubEvent ::ev "a")
                                                    (->StubEvent ::ev "b")
                                                    (->StubEvent ::ev "c")])
                       (is (= 3 (count @captured))))))

(deftest publish-all-empty-list-is-noop
         (testing "publish-all with an empty list does not invoke any handler"
                  (let [publisher (pub/in-process-publisher)
                        called?   (atom false)]
                       (pub/subscribe! publisher ::ev (fn [_] (reset! called? true)))
                       (port/publish-all publisher [])
                       (is (not @called?)))))
