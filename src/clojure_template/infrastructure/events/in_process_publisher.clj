(ns clojure-template.infrastructure.events.in-process-publisher
    "Synchronous in-process IDomainEventPublisher implementation."
    (:require [clojure-template.domain.ports.outbound.event-publisher :as port]
              [clojure-template.domain.events.base :as base]
              [taoensso.timbre :as log]))

(defrecord InProcessEventPublisher [handlers]
  ;; handlers: atom containing map of event-type-kw -> [handler-fn ...]
  port/IDomainEventPublisher
  (publish [_ event]
    (let [etype (base/event-type event)]
         (log/debug "Publishing event" {:type etype :aggregate-id (base/event-aggregate-id event)})
         (doseq [h (get @handlers etype [])]
                (h event))))
  (publish-all [this events]
    (doseq [e events]
           (port/publish this e))))

(defn in-process-publisher
      "Creates a publisher. handlers-map is {event-type-kw [fn ...]}."
      ([] (->InProcessEventPublisher (atom {})))
      ([handlers-map] (->InProcessEventPublisher (atom handlers-map))))

(defn subscribe!
      "Registers handler-fn for event-type on publisher."
      [publisher event-type handler-fn]
      (swap! (:handlers publisher) update event-type (fnil conj []) handler-fn))
