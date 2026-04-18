(ns clojure-template.domain.specifications.base
    "ISpecification protocol with AND/OR/NOT combinators.")

(defprotocol ISpecification
  "Composable predicate over domain objects."
  (satisfied-by? [this candidate] "Returns true if candidate satisfies this specification."))

(defrecord AndSpecification [left right]
  ISpecification
  (satisfied-by? [_ c] (and (satisfied-by? left c) (satisfied-by? right c))))

(defrecord OrSpecification [left right]
  ISpecification
  (satisfied-by? [_ c] (or (satisfied-by? left c) (satisfied-by? right c))))

(defrecord NotSpecification [spec]
  ISpecification
  (satisfied-by? [_ c] (not (satisfied-by? spec c))))

(defn and-spec [left right] (->AndSpecification left right))
(defn or-spec  [left right] (->OrSpecification left right))
(defn not-spec [spec]       (->NotSpecification spec))
