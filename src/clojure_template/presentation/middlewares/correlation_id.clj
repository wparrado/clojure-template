(ns clojure-template.presentation.middlewares.correlation-id
    "Ring middleware that injects a correlation ID into each request."
    (:import [java.util UUID]))

(defn wrap-correlation-id
      "Adds :correlation-id to the request map (from X-Correlation-ID header or new UUID)."
      [handler]
      (fn [request]
          (let [cid  (or (get-in request [:headers "x-correlation-id"])
                         (str (UUID/randomUUID)))
                req  (assoc request :correlation-id cid)
                resp (handler req)]
               (assoc-in resp [:headers "X-Correlation-ID"] cid))))
