(ns clojure-template.application.commands.item-commands
    "Factory functions for Item commands — return plain maps with :type.")

(defn ->CreateItemCommand [name description price]
      {:type :create-item :name name :description description :price price})

(defn ->UpdateItemCommand
      "Only includes fields that are non-nil — nil means 'leave unchanged'."
      [id name description price]
      (cond-> {:type :update-item :id id}
              (some? name)        (assoc :name name)
              (some? description) (assoc :description description)
              (some? price)       (assoc :price price)))

(defn ->DeleteItemCommand [id]
      {:type :delete-item :id id})
