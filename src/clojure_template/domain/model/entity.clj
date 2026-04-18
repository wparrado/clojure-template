(ns clojure-template.domain.model.entity
    "IEntity protocol and helpers for aggregate identity and timestamps.")

(defprotocol IEntity
  "Protocol for all domain entities."
  (entity-id [this] "Returns the unique string UUID of the entity.")
  (created-at [this] "Returns the creation instant.")
  (updated-at [this] "Returns the last-updated instant.")
  (deleted? [this] "Returns true if the entity is soft-deleted."))

(defn entity=
      "Entities are equal by identity (id)."
      [a b]
      (= (entity-id a) (entity-id b)))
