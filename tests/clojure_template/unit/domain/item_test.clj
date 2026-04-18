(ns clojure-template.unit.domain.item-test
    "Unit tests for the Item aggregate invariants, VO validation, and event emission."
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.domain.model.catalog.item :as item]
              [clojure-template.domain.model.catalog.value-objects :as vo]
              [clojure-template.domain.model.aggregate :as agg]
              [clojure-template.domain.exceptions.domain-errors :as errors])
    (:import [clojure_template.domain.model.catalog.item_events ItemCreated ItemUpdated ItemDeleted]
             [java.time Instant]))

(def sample-now (Instant/now))

(defn make-item []
      (item/create-item {:name "Widget" :price 9.99 :now sample-now}))

(deftest create-item-emits-event
         (testing "create-item emits ItemCreated"
                  (let [i (make-item)]
                       (is (= 1 (count (:events i))))
                       (is (instance? ItemCreated (first (:events i)))))))

(deftest collect-events-clears-events
         (testing "collect-events returns [item-no-events, events]"
                  (let [i          (make-item)
                        [i2 evts-] (agg/collect-events i)]
                       (is (empty? (:events i2)))
                       (is (= 1 (count evts-))))))

(deftest item-name-validation
         (testing "empty name throws validation error"
                  (is (thrown? clojure.lang.ExceptionInfo
                               (vo/->item-name ""))))
         (testing "nil name throws validation error"
                  (is (thrown? clojure.lang.ExceptionInfo
                               (vo/->item-name nil))))
         (testing "name too long throws validation error"
                  (is (thrown? clojure.lang.ExceptionInfo
                               (vo/->item-name (apply str (repeat 256 "a")))))))

(deftest item-price-validation
         (testing "negative price throws validation error"
                  (is (thrown? clojure.lang.ExceptionInfo
                               (vo/->item-price -1))))
         (testing "zero price is valid"
                  (is (= 0M (:value (vo/->item-price 0))))))

(deftest update-item-emits-event
         (testing "update-item emits ItemUpdated"
                  (let [i   (make-item)
                        upd (item/update-item i {:name "Gadget" :now sample-now})]
                       (is (= 2 (count (:events upd))))
                       (is (instance? ItemUpdated (last (:events upd)))))))

(deftest delete-item-emits-event
         (testing "delete-item sets deleted=true and emits ItemDeleted"
                  (let [i   (make-item)
                        del (item/delete-item i {:now sample-now})]
                       (is (:deleted del))
                       (is (instance? ItemDeleted (last (:events del)))))))

(deftest delete-already-deleted-throws
         (testing "deleting an already deleted item throws"
                  (let [i   (make-item)
                        del (item/delete-item i {:now sample-now})]
                       (is (thrown? clojure.lang.ExceptionInfo
                                    (item/delete-item del {:now sample-now}))))))

(deftest update-deleted-item-throws
         (testing "update-item on a deleted item throws not-found error"
                  (let [i   (make-item)
                        del (item/delete-item i {:now sample-now})]
                       (is (thrown? clojure.lang.ExceptionInfo
                                    (item/update-item del {:name "New Name" :now sample-now})))
                       (try
                        (item/update-item del {:name "New Name" :now sample-now})
                        (catch clojure.lang.ExceptionInfo e
                               (is (errors/not-found? e)))))))
