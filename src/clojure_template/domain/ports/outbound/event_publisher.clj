(ns clojure-template.domain.ports.outbound.event-publisher
    "IDomainEventPublisher outbound port.")

(defprotocol IDomainEventPublisher
  "Publishes domain events to subscribers."
  (publish [this event] "Publishes a single domain event.")
  (publish-all [this events] "Publishes a sequence of domain events."))
