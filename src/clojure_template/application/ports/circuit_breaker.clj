(ns clojure-template.application.ports.circuit-breaker
    "ICircuitBreaker port — resilience concern owned by the application layer.")

(defprotocol ICircuitBreaker
  "Wraps calls in circuit-breaker logic."
  (call  [this f] "Executes f, applying circuit-breaker policy. Returns result or throws.")
  (open? [this]   "Returns true if the circuit is open (failing fast).")
  (reset [this]   "Resets the circuit breaker to closed state."))
