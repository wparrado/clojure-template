(ns clojure-template.domain.ports.inbound.clock
    "IClock port — abstracts the system clock for testability.")

(defprotocol IClock
  "Provides the current time."
  (now [this] "Returns the current time as a java.time.Instant."))
