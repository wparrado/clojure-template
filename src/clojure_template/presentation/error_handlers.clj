(ns clojure-template.presentation.error-handlers
    "Maps domain errors to HTTP status codes and response bodies."
    (:require [clojure-template.application.exceptions :as errors]))

(defn error->status
      "Returns the HTTP status code for a domain exception."
      [e]
      (cond
       (errors/not-found? e)       404
       (errors/validation-error? e) 422
       (errors/conflict-error? e)  409
       :else                        500))

(defn error->response
      "Builds a Ring response map from a domain exception.
   Content-Type is handled by muuntaja format negotiation."
      [e]
      (let [status (error->status e)
            data   (ex-data e)]
           {:status status
            :body   {:error   (.getMessage e)
                     :type    (some-> data :type name)
                     :details (dissoc data :type)}}))
