(ns clojure-template.application.dtos.pagination
    "PaginatedResult DTO.")

(defrecord PaginatedResult [data total limit offset has-next has-previous])

(defn paginate
      "Builds a PaginatedResult from raw data."
      [data total limit offset]
      (->PaginatedResult
       data
       total
       limit
       offset
       (< (+ offset limit) total)
       (> offset 0)))
