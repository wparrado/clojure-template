(ns clojure-template.unit.presentation.item-schemas-test
    "Unit tests for malli schemas in the presentation layer."
    (:require [clojure.test :refer [deftest is]]
              [malli.core :as m]
              [clojure-template.presentation.api.v1.schemas.item-schemas :as schemas]))

;; ── CreateItemRequest ──────────────────────────────────────────────────────────

(deftest create-request-valid
         (is (m/validate schemas/CreateItemRequest {:name "X" :price 9.99})))

(deftest create-request-missing-name
         (is (not (m/validate schemas/CreateItemRequest {:price 9.99}))))

(deftest create-request-negative-price
         (is (not (m/validate schemas/CreateItemRequest {:name "X" :price -1.0}))))

(deftest create-request-with-description
         (is (m/validate schemas/CreateItemRequest {:name "X" :description "Desc" :price 1.0})))

;; ── UpdateItemRequest ──────────────────────────────────────────────────────────

(deftest update-request-all-optional
         (is (m/validate schemas/UpdateItemRequest {})))

(deftest update-request-valid-name
         (is (m/validate schemas/UpdateItemRequest {:name "New"})))

;; ── ItemIdParam ────────────────────────────────────────────────────────────────

(deftest item-id-param-valid-uuid
         (is (m/validate schemas/ItemIdParam "550e8400-e29b-41d4-a716-446655440000")))

(deftest item-id-param-too-short
         (is (not (m/validate schemas/ItemIdParam "550e8400-e29b-41d4-a716-44665544000"))))
