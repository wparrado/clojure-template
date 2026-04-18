(ns clojure-template.unit.presentation.item-schema-mapper-test
    "Unit tests for the item-schema-mapper (HTTP ↔ application layer)."
    (:require [clojure.test :refer [deftest is]]
              [clojure-template.presentation.mappers.item-schema-mapper :as sut]
              [clojure-template.application.dtos.item-dtos :as dtos]
              [clojure-template.application.dtos.pagination :as pagination])
    (:import [java.time Instant]))

(def ^:private now (Instant/now))

(defn- make-dto
       ([] (make-dto 9.99M))
       ([price]
        (dtos/->ItemDto "uuid-123" "Widget" "A widget" price true now now)))

;; ── create-request->command ────────────────────────────────────────────────────

(deftest create-request->command-maps-all-fields
         (let [cmd (sut/create-request->command {:name "X" :description "D" :price 9.99})]
              (is (= :create-item (:type cmd)))
              (is (= "X" (:name cmd)))
              (is (= "D" (:description cmd)))
              (is (= 9.99 (:price cmd)))))

(deftest create-request->command-nil-description
         (let [cmd (sut/create-request->command {:name "X" :price 9.99})]
              (is (= "X" (:name cmd)))
              (is (nil? (:description cmd)))))

;; ── update-request->command ────────────────────────────────────────────────────

(deftest update-request->command-maps-id-and-fields
         (let [cmd (sut/update-request->command "id-1" {:name "New" :price 5.0})]
              (is (= :update-item (:type cmd)))
              (is (= "id-1" (:id cmd)))
              (is (= "New" (:name cmd)))
              (is (= 5.0 (:price cmd)))))

(deftest update-request->command-partial-only-name
         (let [cmd (sut/update-request->command "id-1" {:name "Only"})]
              (is (= "Only" (:name cmd)))
              (is (not (contains? cmd :price)))
              (is (not (contains? cmd :description)))))

;; ── dto->response ─────────────────────────────────────────────────────────────

(deftest dto->response-maps-scalar-fields
         (let [dto  (make-dto)
               resp (sut/dto->response dto)]
              (is (= "uuid-123" (:id resp)))
              (is (= "Widget" (:name resp)))
              (is (= "A widget" (:description resp)))
              (is (= "9.99" (:price resp)))
              (is (true? (:active resp)))
              (is (string? (:created-at resp)))
              (is (string? (:updated-at resp)))))

(deftest dto->response-nil-price
         (let [dto  (make-dto nil)
               resp (sut/dto->response dto)]
              (is (nil? (:price resp)))))

;; ── search-request->query ──────────────────────────────────────────────────────

(deftest search-request->query-maps-all-params
         (let [qry (sut/search-request->query
                    {:name-contains "X" :min-price 1.0 :max-price 10.0 :limit 5 :offset 0})]
              (is (= :search-items (:type qry)))
              (is (= "X" (:name-contains qry)))
              (is (= 1.0 (:min-price qry)))
              (is (= 10.0 (:max-price qry)))
              (is (= 5 (:limit qry)))
              (is (= 0 (:offset qry)))))

;; ── paginated->response ────────────────────────────────────────────────────────

(deftest paginated->response-maps-pagination-fields
         (let [dto   (make-dto)
               page  (pagination/paginate [dto] 1 10 0)
               resp  (sut/paginated->response page)]
              (is (= 1 (count (:data resp))))
              (is (= 1 (:total resp)))
              (is (= 10 (:limit resp)))
              (is (= 0 (:offset resp)))
              (is (false? (:has-next resp)))
              (is (false? (:has-previous resp)))))
