(ns clojure-template.application.mappers.item-mapper
    "Maps between Item aggregate and ItemDto."
    (:require [clojure-template.application.dtos.item-dtos :as dtos]))

(defn item->dto
      "Converts an Item aggregate to an ItemDto."
      [item]
      (dtos/->ItemDto
       (:id item)
       (some-> item :name :value)
       (some-> item :description :value)
       (some-> item :price :value)
       (:active item)
       (:created-at item)
       (:updated-at item)))
