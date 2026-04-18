(ns clojure-template.domain.model.catalog.value-objects
    "Value objects for the Item aggregate: ItemId, ItemName, ItemDescription, ItemPrice."
    (:require [clojure-template.domain.model.value-object :as vo]
              [clojure-template.domain.exceptions.domain-errors :as errors]
              [clojure.string :as str])
    (:import [java.util UUID]))

;; ── ItemId ────────────────────────────────────────────────────────────────────

(defrecord ItemId [value]
  vo/IValueObject
  (vo-value [_] value))

(defn ->item-id
      "Factory: validates UUID string format, returns ItemId."
      [v]
      (when (nil? v)
            (throw (errors/validation-error "ItemId cannot be nil" {:field :id})))
      (try
       (UUID/fromString (str v))
       (catch Exception _
              (throw (errors/validation-error (str "ItemId must be a valid UUID: " v) {:field :id :value v}))))
      (->ItemId (str v)))

(defn new-item-id
      "Generates a new random ItemId."
      []
      (->ItemId (str (UUID/randomUUID))))

;; ── ItemName ──────────────────────────────────────────────────────────────────

(defrecord ItemName [value]
  vo/IValueObject
  (vo-value [_] value))

(defn ->item-name
      "Factory: validates non-empty string, max 255 chars."
      [v]
      (when (nil? v)
            (throw (errors/validation-error "ItemName cannot be nil" {:field :name})))
      (let [trimmed (str/trim (str v))]
           (when (empty? trimmed)
                 (throw (errors/validation-error "ItemName cannot be blank" {:field :name})))
           (when (> (count trimmed) 255)
                 (throw (errors/validation-error "ItemName cannot exceed 255 characters" {:field :name :length (count trimmed)})))
           (->ItemName trimmed)))

;; ── ItemDescription ───────────────────────────────────────────────────────────

(defrecord ItemDescription [value]
  vo/IValueObject
  (vo-value [_] value))

(defn ->item-description
      "Factory: description is optional — nil is a valid value representing absence.
   If a value is provided it must not exceed 1000 characters."
      [v]
      (when (some? v)
            (when (> (count (str v)) 1000)
                  (throw (errors/validation-error "ItemDescription cannot exceed 1000 characters"
                                                  {:field :description :length (count (str v))}))))
      (->ItemDescription v))

;; ── ItemPrice ─────────────────────────────────────────────────────────────────

(defrecord ItemPrice [value]
  vo/IValueObject
  (vo-value [_] value))

(defn ->item-price
      "Factory: validates non-negative BigDecimal."
      [v]
      (when (nil? v)
            (throw (errors/validation-error "ItemPrice cannot be nil" {:field :price})))
      (let [bd (bigdec v)]
           (when (neg? bd)
                 (throw (errors/validation-error "ItemPrice cannot be negative" {:field :price :value v})))
           (->ItemPrice bd)))
