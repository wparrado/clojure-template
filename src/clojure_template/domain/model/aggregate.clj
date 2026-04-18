(ns clojure-template.domain.model.aggregate
    "IAggregateRoot protocol and helpers.")

(defprotocol IAggregateRoot
  "Protocol for aggregate roots — entities that manage domain events."
  (pending-events [this] "Returns the vector of uncommitted domain events.")
  (clear-events [this] "Returns the aggregate with an empty events vector."))

(defn collect-events
      "Returns [aggregate-with-empty-events, collected-events]."
      [aggregate]
      [(clear-events aggregate) (pending-events aggregate)])
