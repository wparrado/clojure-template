(ns clojure-template.domain.model.catalog.item-events
    "Domain events emitted by the Item aggregate."
    (:require [clojure-template.domain.events.base :as base]))

(defrecord ItemCreated [aggregate-id occurred-at name price description]
  base/IDomainEvent
  (event-type [_] ::item-created)
  (event-aggregate-id [_] aggregate-id)
  (event-occurred-at [_] occurred-at))

(defrecord ItemUpdated [aggregate-id occurred-at name price description]
  base/IDomainEvent
  (event-type [_] ::item-updated)
  (event-aggregate-id [_] aggregate-id)
  (event-occurred-at [_] occurred-at))

(defrecord ItemDeleted [aggregate-id occurred-at]
  base/IDomainEvent
  (event-type [_] ::item-deleted)
  (event-aggregate-id [_] aggregate-id)
  (event-occurred-at [_] occurred-at))
