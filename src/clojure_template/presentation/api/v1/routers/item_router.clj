(ns clojure-template.presentation.api.v1.routers.item-router
    "Ring/reitit route handlers for /api/v1/items."
    (:require [clojure-template.application.ports.item-application-service :as svc-port]
              [clojure-template.application.queries.item-queries :as qrys]
              [clojure-template.application.commands.item-commands :as item-cmds]
              [clojure-template.application.result :as result]
              [clojure-template.presentation.error-handlers :as err]
              [clojure-template.presentation.mappers.item-schema-mapper :as mapper]
              [clojure-template.presentation.api.v1.schemas.item-schemas :as schemas]))

(defn- ok-response [body]
       {:status 200 :body body})

(defn- created-response [body]
       {:status 201 :body body})

(defn- result->response [r success-fn]
       (if (result/success? r)
           (success-fn (:value r))
           (err/error->response (:error r))))

;; ── Handlers ──────────────────────────────────────────────────────────────────

(defn create-item-handler [svc]
      (fn [request]
          (let [body (get-in request [:parameters :body])
                cmd  (mapper/create-request->command body)
                res  (svc-port/create-item svc cmd)]
               (result->response res #(created-response (mapper/dto->response %))))))

(defn list-items-handler [svc]
      (fn [request]
          (let [{:keys [limit offset]} (get-in request [:parameters :query])
                qry (qrys/->ListItemsQuery limit offset)
                res (svc-port/list-items svc qry)]
               (result->response res #(ok-response (mapper/paginated->response %))))))

(defn get-item-handler [svc]
      (fn [request]
          (let [id  (get-in request [:parameters :path :id])
                qry (qrys/->GetItemQuery id)
                res (svc-port/get-item svc qry)]
               (result->response res #(ok-response (mapper/dto->response %))))))

(defn update-item-handler [svc]
      (fn [request]
          (let [id   (get-in request [:parameters :path :id])
                body (get-in request [:parameters :body])
                cmd  (mapper/update-request->command id body)
                res  (svc-port/update-item svc cmd)]
               (result->response res #(ok-response (mapper/dto->response %))))))

(defn delete-item-handler [svc]
      (fn [request]
          (let [id  (get-in request [:parameters :path :id])
                cmd (item-cmds/->DeleteItemCommand id)
                res (svc-port/delete-item svc cmd)]
               (result->response res (fn [_] {:status 204 :body nil})))))

(defn search-items-handler [svc]
      (fn [request]
          (let [params (get-in request [:parameters :query])
                qry    (mapper/search-request->query params)
                res    (svc-port/search-items svc qry)]
               (result->response res #(ok-response (mapper/paginated->response %))))))

;; ── Routes ────────────────────────────────────────────────────────────────────

(defn item-routes [svc]
      ["/api/v1/items"
       ["" {:get  {:handler    (list-items-handler svc)
                   :parameters {:query {:limit  [:int {:default 20 :min 1 :max 100}]
                                        :offset [:int {:default 0  :min 0}]}}}
            :post {:handler    (create-item-handler svc)
                   :parameters {:body schemas/CreateItemRequest}}}]
       ["/search" {:get {:handler    (search-items-handler svc)
                         :parameters {:query schemas/SearchQueryParams}}}]
       ["/:id" {:get    {:handler    (get-item-handler svc)
                         :parameters {:path {:id schemas/ItemIdParam}}}
                :put    {:handler    (update-item-handler svc)
                         :parameters {:path {:id schemas/ItemIdParam}
                                      :body schemas/UpdateItemRequest}}
                :delete {:handler    (delete-item-handler svc)
                         :parameters {:path {:id schemas/ItemIdParam}}}}]])
