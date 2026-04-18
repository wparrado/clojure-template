(ns clojure-template.infrastructure.persistence.in-memory.item-repository
    "In-memory atom-based IItemRepository implementation."
    (:require [clojure-template.domain.ports.outbound.item-repository :as port]
              [clojure-template.domain.specifications.base :as spec]))

(defrecord InMemoryItemRepository [store]
  ;; store: atom containing {id-string -> Item}
  port/IItemRepository

  (save [_ item]
    (swap! store assoc (:id item) item)
    item)

  (find-by-id [_ id]
    (get @store id))

  (find-all [_ limit offset]
    (let [all-active (->> (vals @store)
                          (remove :deleted)
                          (sort-by :created-at)
                          vec)
          total      (count all-active)
          page       (->> all-active (drop offset) (take limit) vec)]
         {:items page :total total}))

  (find-matching [_ spec-val limit offset]
    (let [matching (->> (vals @store)
                        (filter #(spec/satisfied-by? spec-val %))
                        (sort-by :created-at)
                        vec)
          total    (count matching)
          page     (->> matching (drop offset) (take limit) vec)]
         {:items page :total total}))

  (count-all [_]
    (->> (vals @store) (remove :deleted) count)))

(defn in-memory-item-repository
      "Creates a fresh in-memory item repository."
      []
      (->InMemoryItemRepository (atom {})))
