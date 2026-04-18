(ns clojure-template.unit.application.item-mapper-test
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.domain.model.catalog.item :as item]
              [clojure-template.application.mappers.item-mapper :as mapper])
    (:import [java.time Instant]))

(def ^:private now (Instant/now))

(deftest item->dto-maps-all-fields
         (testing "item->dto extracts scalar values from all VO fields"
                  (let [i   (item/create-item {:name "Widget" :price 9.99 :now now
                                               :description "A fine widget"})
                        dto (mapper/item->dto i)]
                       (is (instance? clojure_template.application.dtos.item_dtos.ItemDto dto))
                       (is (string? (:id dto)))
                       (is (= "Widget" (:name dto)))
                       (is (= "A fine widget" (:description dto)))
                       (is (= 9.99M (:price dto)))
                       (is (true? (:active dto)))
                       (is (= now (:created-at dto)))
                       (is (= now (:updated-at dto))))))

(deftest item->dto-handles-nil-description
         (testing "item->dto maps nil description correctly"
                  (let [i   (item/create-item {:name "NoDesc" :price 1.0 :now now})
                        dto (mapper/item->dto i)]
                       (is (nil? (:description dto))))))

(deftest item->dto-returns-scalar-not-record
         (testing "dto fields are plain scalars, not ValueObject records"
                  (let [i   (item/create-item {:name "Scalar" :price 5.5 :now now})
                        dto (mapper/item->dto i)]
                       (is (not (map? (class (:name dto))))
                           "name should be a plain String")
                       (is (instance? java.math.BigDecimal (:price dto))))))
