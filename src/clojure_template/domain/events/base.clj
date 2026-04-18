(ns clojure-template.domain.events.base
    "Base protocol and helpers for domain events.")

(defprotocol IDomainEvent
  "Marker protocol for all domain events."
  (event-type [this] "Returns the keyword type of the event.")
  (event-aggregate-id [this] "Returns the aggregate id this event belongs to.")
  (event-occurred-at [this] "Returns the instant when the event occurred."))
