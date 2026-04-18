(ns clojure-template.infrastructure.clock.system-clock
    "IClock implementations: SystemClock and FakeClock."
    (:require [clojure-template.domain.ports.inbound.clock :as clock])
    (:import [java.time Instant]))

(defrecord SystemClock []
  clock/IClock
  (now [_] (Instant/now)))

(defrecord FakeClock [fixed-time]
  clock/IClock
  (now [_] @fixed-time))

(defn system-clock [] (->SystemClock))
(defn fake-clock [^Instant t] (->FakeClock (atom t)))
(defn advance-fake-clock! [^FakeClock fc millis]
      (swap! (:fixed-time fc) #(.plusMillis ^Instant % millis)))
