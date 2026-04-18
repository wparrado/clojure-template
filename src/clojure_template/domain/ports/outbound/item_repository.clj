(ns clojure-template.domain.ports.outbound.item-repository
    "IItemRepository outbound port.")

(defprotocol IItemRepository
  "Persistence port for Item aggregates."
  (save [this item] "Upserts the item. Returns the saved item.")
  (find-by-id [this id] "Returns the Item with the given id string, or nil.
             NOTE: may return soft-deleted items — callers are responsible for checking :deleted.")
  (find-all [this limit offset] "Returns a map {:items [...] :total N} of non-deleted items.")
  (find-matching [this spec limit offset] "Returns items satisfying the ISpecification.")
  (count-all [this] "Returns the count of non-deleted items."))
