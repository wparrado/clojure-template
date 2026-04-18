(ns clojure-template.presentation.api.v1.schemas.item-schemas
    "Malli/reitit schemas for Item request validation.")

(def CreateItemRequest
     [:map
      [:name        [:string {:min 1 :max 255}]]
      [:description {:optional true} [:maybe [:string {:max 1000}]]]
      [:price       [:or [:double {:min 0}] [:int {:min 0}]]]])

(def UpdateItemRequest
     [:map
      [:name        {:optional true} [:string {:min 1 :max 255}]]
      [:description {:optional true} [:maybe [:string {:max 1000}]]]
      [:price       {:optional true} [:or [:double {:min 0}] [:int {:min 0}]]]])

(def SearchQueryParams
     [:map
      [:name-contains {:optional true} [:string {:min 1}]]
      [:min-price     {:optional true} [:double {:min 0}]]
      [:max-price     {:optional true} [:double {:min 0}]]
      [:limit         {:optional true} [:int {:default 20 :min 1 :max 100}]]
      [:offset        {:optional true} [:int {:default 0  :min 0}]]])

(def ItemIdParam
     [:string {:min 36 :max 36}])
