(ns clojure-template.unit.domain.specifications-test
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.domain.model.catalog.item :as item]
              [clojure-template.domain.specifications.base :as base]
              [clojure-template.domain.specifications.item-specifications :as specs])
    (:import [java.time Instant]))

(def ^:private now (Instant/now))

(defn- make-item
       ([] (make-item {}))
       ([overrides]
        (merge (item/create-item {:name "Widget" :price 10.0 :now now}) overrides)))

;; ── ActiveItemSpec ────────────────────────────────────────────────────────────

(deftest active-item-spec-true-for-active-not-deleted
         (testing "ActiveItemSpec satisfied by active, non-deleted item"
                  (let [i    (make-item)
                        spec (specs/active-item-spec)]
                       (is (base/satisfied-by? spec i)))))

(deftest active-item-spec-false-for-deleted
         (testing "ActiveItemSpec not satisfied by deleted item"
                  (let [i    (make-item {:deleted true})
                        spec (specs/active-item-spec)]
                       (is (not (base/satisfied-by? spec i))))))

(deftest active-item-spec-false-for-inactive
         (testing "ActiveItemSpec not satisfied by inactive item"
                  (let [i    (make-item {:active false})
                        spec (specs/active-item-spec)]
                       (is (not (base/satisfied-by? spec i))))))

;; ── NameContainsSpec ──────────────────────────────────────────────────────────

(deftest name-contains-spec-true-case-insensitive
         (testing "NameContainsSpec matches substring case-insensitively"
                  (let [i    (make-item)
                        spec (specs/name-contains-spec "WIDGET")]
                       (is (base/satisfied-by? spec i)))))

(deftest name-contains-spec-false-when-not-contained
         (testing "NameContainsSpec does not match unrelated substring"
                  (let [i    (make-item)
                        spec (specs/name-contains-spec "Gadget")]
                       (is (not (base/satisfied-by? spec i))))))

;; ── PriceRangeSpec ────────────────────────────────────────────────────────────

(deftest price-range-spec-true-when-in-range
         (testing "PriceRangeSpec satisfied when price is within [min, max]"
                  (let [i    (make-item)
                        spec (specs/price-range-spec 5.0 20.0)]
                       (is (base/satisfied-by? spec i)))))

(deftest price-range-spec-false-when-out-of-range
         (testing "PriceRangeSpec not satisfied when price is above max"
                  (let [i    (make-item)
                        spec (specs/price-range-spec 20.0 50.0)]
                       (is (not (base/satisfied-by? spec i))))))

;; ── Combinators ───────────────────────────────────────────────────────────────

(deftest and-spec-both-must-be-true
         (testing "and-spec: true AND true => true"
                  (let [i    (make-item)
                        spec (base/and-spec (specs/active-item-spec)
                                            (specs/name-contains-spec "widget"))]
                       (is (base/satisfied-by? spec i))))
         (testing "and-spec: true AND false => false"
                  (let [i    (make-item)
                        spec (base/and-spec (specs/active-item-spec)
                                            (specs/name-contains-spec "Gadget"))]
                       (is (not (base/satisfied-by? spec i))))))

(deftest or-spec-at-least-one-must-be-true
         (testing "or-spec: false OR true => true"
                  (let [i    (make-item)
                        spec (base/or-spec (specs/name-contains-spec "Gadget")
                                           (specs/active-item-spec))]
                       (is (base/satisfied-by? spec i))))
         (testing "or-spec: false OR false => false"
                  (let [i    (make-item {:deleted true :active false})
                        spec (base/or-spec (specs/active-item-spec)
                                           (specs/name-contains-spec "Gadget"))]
                       (is (not (base/satisfied-by? spec i))))))

(deftest not-spec-negates-spec
         (testing "not-spec: negates a satisfied spec"
                  (let [i    (make-item)
                        spec (base/not-spec (specs/name-contains-spec "Gadget"))]
                       (is (base/satisfied-by? spec i))))
         (testing "not-spec: negates a non-satisfied spec"
                  (let [i    (make-item)
                        spec (base/not-spec (specs/active-item-spec))]
                       (is (not (base/satisfied-by? spec i))))))
