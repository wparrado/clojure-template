(ns clojure-template.application.result
    "Railway-oriented Result type: Success and Failure.")

(defrecord Success [value])
(defrecord Failure [error])

(defn ok  [v] (->Success v))
(defn err [e] (->Failure e))

(defn success? [r] (instance? Success r))
(defn failure? [r] (instance? Failure r))

(defn unwrap
      "Returns value on Success; throws the error on Failure."
      [r]
      (if (success? r)
          (:value r)
          (throw (:error r))))

(defn map-ok
      "Applies f to the value inside a Success, leaving Failure unchanged."
      [r f]
      (if (success? r)
          (ok (f (:value r)))
          r))

(defn flat-map
      "Monadic bind: applies f (returns Result) to Success value."
      [r f]
      (if (success? r)
          (f (:value r))
          r))

(defmacro try-result
          "Wraps body in try/catch, returning ok(result) or err(exception).
   Only catches ExceptionInfo (domain errors); unexpected exceptions propagate."
          [& body]
          `(try
            (ok (do ~@body))
            (catch clojure.lang.ExceptionInfo e#
                   (err e#))))

(defmacro with-uow
          "Executes body within a fresh unit of work produced by uow-factory.
   Binds the UoW to `uow-sym`, calls commit on success, rollback on any
   exception (re-throwing it so callers can still handle it).

   Usage:
     (with-uow [uow (uow-factory)]
       (let [repo (uow-port/get-item-repository uow)]
         ...))"
          [[uow-sym uow-expr] & body]
          `(let [~uow-sym ~uow-expr]
                (try
                 (let [result# (do ~@body)]
                      (clojure-template.application.ports.unit-of-work/commit ~uow-sym)
                      result#)
                 (catch Exception e#
                        (clojure-template.application.ports.unit-of-work/rollback ~uow-sym)
                        (throw e#)))))
