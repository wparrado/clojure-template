(ns clojure-template.application.queries.item-queries
    "Factory functions for Item queries — return plain maps with :type.")

(defn ->GetItemQuery [id]
      {:type :get-item :id id})

(defn ->ListItemsQuery [limit offset]
      {:type :list-items :limit limit :offset offset})

(defn ->SearchItemsQuery [name-contains min-price max-price limit offset]
      {:type :search-items
       :name-contains name-contains
       :min-price min-price
       :max-price max-price
       :limit limit
       :offset offset})
