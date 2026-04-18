(ns clojure-template.infrastructure.resilience.circuit-breaker
    "Simple ICircuitBreaker implementation."
    (:require [clojure-template.application.ports.circuit-breaker :as port]))

(defn- now-ms [] (System/currentTimeMillis))

(defrecord SimpleCircuitBreaker [state failure-threshold reset-timeout-ms]
  ;; state: atom {:status :closed|:open|:half-open :failures N :opened-at nil|ms}
  port/ICircuitBreaker

  (call [_ f]
    (let [{:keys [status opened-at]} @state]
         (cond
          (= status :closed)
          (try
           (let [r (f)]
                (swap! state assoc :failures 0)
                r)
           (catch Exception e
                  (swap! state update :failures inc)
                  (when (>= (:failures @state) failure-threshold)
                        (swap! state assoc :status :open :opened-at (now-ms)))
                  (throw e)))

          (= status :open)
          (if (> (- (now-ms) opened-at) reset-timeout-ms)
              (do
               (swap! state assoc :status :half-open)
               (try
                (let [r (f)]
                     (swap! state assoc :status :closed :failures 0 :opened-at nil)
                     r)
                (catch Exception e
                       (swap! state assoc :status :open :opened-at (now-ms))
                       (throw e))))
              (throw (ex-info "Circuit breaker is open" {:type ::circuit-open})))

          (= status :half-open)
          (try
           (let [r (f)]
                (swap! state assoc :status :closed :failures 0 :opened-at nil)
                r)
           (catch Exception e
                  (swap! state assoc :status :open :opened-at (now-ms))
                  (throw e))))))

  (open? [_] (= :open (:status @state)))
  (reset [_] (swap! state assoc :status :closed :failures 0 :opened-at nil)))

(defn simple-circuit-breaker
      "Creates a circuit breaker. failure-threshold defaults to 5, reset-timeout-ms to 60000."
      ([] (simple-circuit-breaker 5 60000))
      ([failure-threshold reset-timeout-ms]
       (->SimpleCircuitBreaker
        (atom {:status :closed :failures 0 :opened-at nil})
        failure-threshold
        reset-timeout-ms)))
