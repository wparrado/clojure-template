(ns clojure-template.domain.model.catalog.item
    "Item aggregate root."
    (:require [clojure-template.domain.model.entity :as entity]
              [clojure-template.domain.model.aggregate :as agg]
              [clojure-template.domain.model.catalog.value-objects :as vo]
              [clojure-template.domain.model.catalog.item-events :as events]
              [clojure-template.domain.exceptions.domain-errors :as errors]))

;; NOTE: :name, :description, and :price are ValueObject records (ItemName,
;;       ItemDescription, ItemPrice), not raw scalars. Access their underlying
;;       values via (:value field) when mapping to/from external representations.
(defrecord Item [id name description price active created-at updated-at deleted events]
  entity/IEntity
  (entity-id [_] id)
  (created-at [_] created-at)
  (updated-at [_] updated-at)
  (deleted? [_] deleted)

  agg/IAggregateRoot
  (pending-events [_] events)
  (clear-events [this] (assoc this :events [])))

(defn create-item
      "Factory: validates inputs, builds Item aggregate, emits ItemCreated.
   now (java.time.Instant) is required — inject via IClock for deterministic behavior."
      [{:keys [id name description price now]}]
      (let [item-id    (if id (vo/->item-id id) (vo/new-item-id))
            item-name  (vo/->item-name name)
            item-desc  (vo/->item-description description)
            item-price (vo/->item-price price)
            evt        (events/->ItemCreated (:value item-id) now
                                             (:value item-name)
                                             (:value item-price)
                                             (:value item-desc))]
           (->Item (:value item-id)
                   item-name
                   item-desc
                   item-price
                   true
                   now
                   now
                   false
                   [evt])))

(defn update-item
      "Updates name/description/price when present in changes, emits ItemUpdated."
      [item {:keys [name description price now] :as changes}]
      (when (:deleted item)
            (throw (errors/not-found-error "Cannot update a deleted item" {:id (:id item)})))
      (let [new-name  (if (contains? changes :name)
                          (vo/->item-name name)
                          (:name item))
            new-desc  (if (contains? changes :description)
                          (vo/->item-description description)
                          (:description item))
            new-price (if (contains? changes :price)
                          (vo/->item-price price)
                          (:price item))
            ts        now
            evt       (events/->ItemUpdated (:id item) ts
                                            (:value new-name)
                                            (:value new-price)
                                            (:value new-desc))]
           (-> item
               (assoc :name new-name :description new-desc :price new-price :updated-at ts)
               (update :events conj evt))))

(defn delete-item
      "Soft-deletes the item, emits ItemDeleted."
      [item {:keys [now]}]
      (when (:deleted item)
            (throw (errors/not-found-error "Item is already deleted" {:id (:id item)})))
      (let [evt (events/->ItemDeleted (:id item) now)]
           (-> item
               (assoc :deleted true :active false :updated-at now)
               (update :events conj evt))))
