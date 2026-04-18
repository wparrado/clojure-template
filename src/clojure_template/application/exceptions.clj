(ns clojure-template.application.exceptions
    "Re-exports domain error constructors for use in application layer."
    (:require [clojure-template.domain.exceptions.domain-errors :as d]))

(def not-found-error    d/not-found-error)
(def validation-error   d/validation-error)
(def conflict-error     d/conflict-error)
(def domain-error       d/domain-error)
(def not-found?         d/not-found?)
(def validation-error?  d/validation-error?)
(def conflict-error?    d/conflict-error?)
(def error-type         d/error-type)
