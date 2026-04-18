(ns clojure-template.unit.application.item-service-test
    "Unit tests for ItemApplicationService — verifies delegation to handlers."
    (:require [clojure.test :refer [deftest is]]
              [clojure-template.application.services.item-service :as svc]
              [clojure-template.application.ports.item-application-service :as port]
              [clojure-template.application.handlers.command-handlers :as cmds]
              [clojure-template.application.handlers.query-handlers :as qrys]
              [clojure-template.application.result :as result]))

(defn- stub-command-handler [return-val]
       (reify cmds/ICommandHandler
              (handle [_ _] return-val)))

(defn- stub-query-handler [return-val]
       (reify qrys/IQueryHandler
              (handle [_ _] return-val)))

(defn- make-service [overrides]
       (let [defaults {:create (stub-command-handler (result/ok :created))
                       :update (stub-command-handler (result/ok :updated))
                       :delete (stub-command-handler (result/ok :deleted))
                       :get    (stub-query-handler   (result/ok :found))
                       :list   (stub-query-handler   (result/ok :listed))
                       :search (stub-query-handler   (result/ok :searched))}
             m        (merge defaults overrides)]
            (svc/->ItemApplicationService
             (:create m) (:update m) (:delete m)
             (:get m)    (:list m)   (:search m))))

(deftest create-item-delegates-to-create-handler
         (let [service (make-service {:create (stub-command-handler (result/ok :my-item))})
               res     (port/create-item service {})]
              (is (result/success? res))
              (is (= :my-item (:value res)))))

(deftest update-item-delegates-to-update-handler
         (let [service (make-service {:update (stub-command-handler (result/ok :updated-item))})
               res     (port/update-item service {})]
              (is (result/success? res))
              (is (= :updated-item (:value res)))))

(deftest delete-item-delegates-to-delete-handler
         (let [service (make-service {:delete (stub-command-handler (result/ok true))})
               res     (port/delete-item service {})]
              (is (result/success? res))
              (is (true? (:value res)))))

(deftest get-item-delegates-to-get-handler
         (let [service (make-service {:get (stub-query-handler (result/ok :the-item))})
               res     (port/get-item service {})]
              (is (result/success? res))
              (is (= :the-item (:value res)))))

(deftest list-items-delegates-to-list-handler
         (let [service (make-service {:list (stub-query-handler (result/ok :page))})
               res     (port/list-items service {})]
              (is (result/success? res))
              (is (= :page (:value res)))))

(deftest search-items-delegates-to-search-handler
         (let [service (make-service {:search (stub-query-handler (result/ok :results))})
               res     (port/search-items service {})]
              (is (result/success? res))
              (is (= :results (:value res)))))

(deftest service-propagates-failure-results
         (let [err     (ex-info "not found" {:type ::not-found})
               service (make-service {:get (stub-query-handler (result/err err))})
               res     (port/get-item service {})]
              (is (result/failure? res))
              (is (= err (:error res)))))
