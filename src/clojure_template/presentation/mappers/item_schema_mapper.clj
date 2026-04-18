(ns clojure-template.presentation.mappers.item-schema-mapper
    "Maps between HTTP request/response schemas and application DTOs/commands."
    (:require [clojure-template.application.commands.item-commands :as cmds]
              [clojure-template.application.queries.item-queries :as qrys]))

(defn create-request->command
      "Maps a parsed create-item request body to CreateItemCommand."
      [{:keys [name description price]}]
      (cmds/->CreateItemCommand name description price))

(defn update-request->command
      "Maps parsed update request + path id to UpdateItemCommand.
   Only includes fields explicitly present in body (partial update semantics)."
      [id body]
      (cond-> {:type :update-item :id id}
              (contains? body :name)        (assoc :name (:name body))
              (contains? body :description) (assoc :description (:description body))
              (contains? body :price)       (assoc :price (:price body))))

(defn dto->response
      "Converts an ItemDto to a JSON-serializable map."
      [dto]
      {:id          (:id dto)
       :name        (:name dto)
       :description (:description dto)
       :price       (when (:price dto) (str (:price dto)))
       :active      (:active dto)
       :created-at  (str (:created-at dto))
       :updated-at  (str (:updated-at dto))})

(defn search-request->query
      "Maps query-string params to SearchItemsQuery."
      [{:keys [name-contains min-price max-price limit offset]}]
      (qrys/->SearchItemsQuery name-contains min-price max-price limit offset))

(defn paginated->response
      "Converts a PaginatedResult<ItemDto> to a JSON-serializable map."
      [page]
      {:data         (map dto->response (:data page))
       :total        (:total page)
       :limit        (:limit page)
       :offset       (:offset page)
       :has-next     (:has-next page)
       :has-previous (:has-previous page)})
