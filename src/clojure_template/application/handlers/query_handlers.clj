(ns clojure-template.application.handlers.query-handlers
    "CQRS query handlers."
    (:require [clojure-template.domain.ports.outbound.item-repository :as repo-port]
              [clojure-template.domain.specifications.item-specifications :as specs]
              [clojure-template.domain.specifications.base :as base-spec]
              [clojure-template.application.result :as result]
              [clojure-template.application.mappers.item-mapper :as mapper]
              [clojure-template.application.dtos.pagination :as pagination]
              [clojure-template.domain.exceptions.domain-errors :as errors]))

(defprotocol IQueryHandler
  "Single-method protocol for query handlers."
  (handle [this qry] "Handles a query, returning a Result."))

;; ── GetItemHandler ────────────────────────────────────────────────────────────

(defrecord GetItemHandler [repo]
  IQueryHandler
  (handle [_ qry]
    (result/try-result
      (let [item (repo-port/find-by-id repo (:id qry))]
           (when (or (nil? item) (:deleted item))
                 (throw (errors/not-found-error "Item not found" {:id (:id qry)})))
           (mapper/item->dto item)))))

;; ── ListItemsHandler ──────────────────────────────────────────────────────────

(defrecord ListItemsHandler [repo]
  IQueryHandler
  (handle [_ qry]
    (result/try-result
      (let [limit  (or (:limit qry) 20)
            offset (or (:offset qry) 0)
            {:keys [items total]} (repo-port/find-all repo limit offset)
            dtos   (map mapper/item->dto items)]
           (pagination/paginate dtos total limit offset)))))

;; ── SearchItemsHandler ────────────────────────────────────────────────────────

(defrecord SearchItemsHandler [repo]
  IQueryHandler
  (handle [_ qry]
    (result/try-result
      (let [limit       (or (:limit qry) 20)
            offset      (or (:offset qry) 0)
            active-spec (specs/active-item-spec)
            spec        (cond-> active-spec
                                (:name-contains qry)
                                (base-spec/and-spec (specs/name-contains-spec (:name-contains qry)))
                                (and (:min-price qry) (:max-price qry))
                                (base-spec/and-spec (specs/price-range-spec (:min-price qry) (:max-price qry))))
            {:keys [items total]} (repo-port/find-matching repo spec limit offset)
            dtos        (map mapper/item->dto items)]
           (pagination/paginate dtos total limit offset)))))
