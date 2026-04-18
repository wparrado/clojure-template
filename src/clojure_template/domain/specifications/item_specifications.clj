(ns clojure-template.domain.specifications.item-specifications
    "Specifications for Item aggregates."
    (:require [clojure-template.domain.specifications.base :as base]
              [clojure.string :as str]))

(defrecord ActiveItemSpec []
  base/ISpecification
  (satisfied-by? [_ item]
    (and (not (:deleted item)) (:active item))))

(defrecord NameContainsSpec [substring]
  base/ISpecification
  (satisfied-by? [_ item]
    (let [name-val (some-> item :name :value str/lower-case)]
         (and (some? name-val)
              (str/includes? name-val (str/lower-case substring))))))

(defrecord PriceRangeSpec [min-price max-price]
  base/ISpecification
  (satisfied-by? [_ item]
    (let [price (some-> item :price :value)]
         (and (some? price)
              (>= price (bigdec min-price))
              (<= price (bigdec max-price))))))

(defn active-item-spec [] (->ActiveItemSpec))
(defn name-contains-spec [s] (->NameContainsSpec s))
(defn price-range-spec [mn mx] (->PriceRangeSpec mn mx))
