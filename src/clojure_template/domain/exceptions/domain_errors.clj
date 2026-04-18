(ns clojure-template.domain.exceptions.domain-errors
    "Domain error hierarchy using ex-info with :type dispatch key.")

(defn domain-error
      "Base domain error."
      [message data]
      (ex-info message (merge {:type ::domain-error} data)))

(defn not-found-error
      "Error for when an aggregate cannot be found."
      [message data]
      (ex-info message (merge {:type ::not-found} data)))

(defn validation-error
      "Error for invalid input or invariant violation."
      [message data]
      (ex-info message (merge {:type ::validation-error} data)))

(defn conflict-error
      "Error for conflicts like duplicate names."
      [message data]
      (ex-info message (merge {:type ::conflict-error} data)))

(defn error-type [e]
      (some-> e ex-data :type))

(defn not-found? [e] (= (error-type e) ::not-found))
(defn validation-error? [e] (= (error-type e) ::validation-error))
(defn conflict-error? [e] (= (error-type e) ::conflict-error))
