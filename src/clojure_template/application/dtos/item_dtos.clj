(ns clojure-template.application.dtos.item-dtos
    "Output DTO for Item.")

(defrecord ItemDto [id name description price active created-at updated-at])
