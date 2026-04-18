(ns clojure-template.domain.model.value-object
    "IValueObject marker protocol. Value objects are equal by value.")

(defprotocol IValueObject
  "Marker protocol for value objects."
  (vo-value [this] "Returns the raw underlying value."))
