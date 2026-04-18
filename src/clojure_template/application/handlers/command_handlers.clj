(ns clojure-template.application.handlers.command-handlers
    "CQRS command handlers: CreateItemHandler, UpdateItemHandler, DeleteItemHandler."
    (:require [clojure-template.domain.model.catalog.item :as item-agg]
              [clojure-template.domain.ports.outbound.item-repository :as repo-port]
              [clojure-template.domain.ports.inbound.clock :as clock-port]
              [clojure-template.application.ports.unit-of-work :as uow-port]
              [clojure-template.application.result :as result]
              [clojure-template.application.mappers.item-mapper :as mapper]
              [clojure-template.domain.exceptions.domain-errors :as errors]))

(defprotocol ICommandHandler
  "Single-method protocol for command handlers."
  (handle [this cmd] "Handles a command, returning a Result."))

;; ── CreateItemHandler ─────────────────────────────────────────────────────────

(defrecord CreateItemHandler [uow-factory clock]
  ICommandHandler
  (handle [_ cmd]
    (result/try-result
      (result/with-uow [uow (uow-factory)]
                       (let [repo  (uow-port/get-item-repository uow)
                             now   (clock-port/now clock)
                             item  (item-agg/create-item {:name        (:name cmd)
                                                          :description (:description cmd)
                                                          :price       (:price cmd)
                                                          :now         now})
                             saved (repo-port/save repo item)]
                            (mapper/item->dto saved))))))

;; ── UpdateItemHandler ─────────────────────────────────────────────────────────

(defrecord UpdateItemHandler [uow-factory clock]
  ICommandHandler
  (handle [_ cmd]
    (result/try-result
      (result/with-uow [uow (uow-factory)]
                       (let [repo     (uow-port/get-item-repository uow)
                             now      (clock-port/now clock)
                             existing (repo-port/find-by-id repo (:id cmd))]
                            (when (nil? existing)
                                  (throw (errors/not-found-error "Item not found" {:id (:id cmd)})))
                            (let [changes (-> (select-keys cmd [:name :description :price])
                                              (assoc :now now))
                                  updated (item-agg/update-item existing changes)
                                  saved   (repo-port/save repo updated)]
                                 (mapper/item->dto saved)))))))

;; ── DeleteItemHandler ─────────────────────────────────────────────────────────

(defrecord DeleteItemHandler [uow-factory clock]
  ICommandHandler
  (handle [_ cmd]
    (result/try-result
      (result/with-uow [uow (uow-factory)]
                       (let [repo     (uow-port/get-item-repository uow)
                             now      (clock-port/now clock)
                             existing (repo-port/find-by-id repo (:id cmd))]
                            (when (nil? existing)
                                  (throw (errors/not-found-error "Item not found" {:id (:id cmd)})))
                            (let [deleted (item-agg/delete-item existing {:now now})]
                                 (repo-port/save repo deleted)
                                 true))))))
