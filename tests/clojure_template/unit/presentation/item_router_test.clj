(ns clojure-template.unit.presentation.item-router-test
    "Unit tests for item-router handlers (no HTTP stack, service stubbed)."
    (:require [clojure.test :refer [deftest is]]
              [clojure-template.presentation.api.v1.routers.item-router :as router]
              [clojure-template.application.ports.item-application-service :as svc-port]
              [clojure-template.application.result :as result]
              [clojure-template.application.dtos.item-dtos :as dtos]
              [clojure-template.application.dtos.pagination :as pagination]
              [clojure-template.application.exceptions :as errors])
    (:import [java.time Instant]))

(def ^:private now (Instant/now))

(def ^:private sample-dto
     (dtos/->ItemDto "uuid-123" "Widget" "A widget" 9.99M true now now))

(def ^:private sample-page
     (pagination/paginate [sample-dto] 1 10 0))

(defn- make-stub-svc [responses]
       (reify svc-port/IItemApplicationService
              (create-item  [_ _] (:create-item responses))
              (update-item  [_ _] (:update-item responses))
              (delete-item  [_ _] (:delete-item responses))
              (get-item     [_ _] (:get-item responses))
              (list-items   [_ _] (:list-items responses))
              (search-items [_ _] (:search-items responses))))

;; ── create-item ────────────────────────────────────────────────────────────────

(deftest create-item-handler-success-returns-201
         (let [svc     (make-stub-svc {:create-item (result/ok sample-dto)})
               handler (router/create-item-handler svc)
               req     {:parameters {:body {:name "Widget" :price 9.99}}}
               resp    (handler req)]
              (is (= 201 (:status resp)))))

(deftest create-item-handler-failure-returns-error-status
         (let [e       (errors/not-found-error "Not found" {})
               svc     (make-stub-svc {:create-item (result/err e)})
               handler (router/create-item-handler svc)
               req     {:parameters {:body {:name "Widget" :price 9.99}}}
               resp    (handler req)]
              (is (= 404 (:status resp)))))

;; ── get-item ───────────────────────────────────────────────────────────────────

(deftest get-item-handler-success-returns-200
         (let [svc     (make-stub-svc {:get-item (result/ok sample-dto)})
               handler (router/get-item-handler svc)
               req     {:parameters {:path {:id "uuid-123"}}}
               resp    (handler req)]
              (is (= 200 (:status resp)))))

(deftest get-item-handler-not-found-returns-404
         (let [e       (errors/not-found-error "Item not found" {:id "uuid-123"})
               svc     (make-stub-svc {:get-item (result/err e)})
               handler (router/get-item-handler svc)
               req     {:parameters {:path {:id "uuid-123"}}}
               resp    (handler req)]
              (is (= 404 (:status resp)))))

;; ── delete-item ────────────────────────────────────────────────────────────────

(deftest delete-item-handler-success-returns-204
         (let [svc     (make-stub-svc {:delete-item (result/ok true)})
               handler (router/delete-item-handler svc)
               req     {:parameters {:path {:id "uuid-123"}}}
               resp    (handler req)]
              (is (= 204 (:status resp)))
              (is (nil? (:body resp)))))

;; ── list-items ─────────────────────────────────────────────────────────────────

(deftest list-items-handler-returns-200
         (let [svc     (make-stub-svc {:list-items (result/ok sample-page)})
               handler (router/list-items-handler svc)
               req     {:parameters {:query {:limit 10 :offset 0}}}
               resp    (handler req)]
              (is (= 200 (:status resp)))))

;; ── update-item ────────────────────────────────────────────────────────────────

(deftest update-item-handler-success-returns-200
         (let [svc     (make-stub-svc {:update-item (result/ok sample-dto)})
               handler (router/update-item-handler svc)
               req     {:parameters {:path {:id "uuid-123"} :body {:name "Updated"}}}
               resp    (handler req)]
              (is (= 200 (:status resp)))))
