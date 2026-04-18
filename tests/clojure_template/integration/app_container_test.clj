(ns clojure-template.integration.app-container-test
    "Integration tests for the composition root and Ring app factory."
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.app-container :as container]
              [clojure-template.main :as main]
              [clojure-template.settings :as settings]))

(defn- test-settings []
       (settings/load-settings))

(deftest make-container-returns-app-container
         (testing "make-container returns an AppContainer record with an :app-service"
                  (let [cfg       (test-settings)
                        app       (container/make-container cfg)]
                       (is (instance? clojure_template.app_container.AppContainer app))
                       (is (some? (:app-service app))))))

(deftest make-container-has-all-sub-containers
         (testing "make-container wires all sub-containers"
                  (let [app (container/make-container (test-settings))]
                       (is (some? (:persistence app)))
                       (is (some? (:events app)))
                       (is (some? (:resilience app)))
                       (is (some? (:clock app))))))

(deftest make-app-returns-ring-handler
         (testing "make-app returns a callable Ring handler"
                  (let [app-container (container/make-container (test-settings))
                        handler       (main/make-app app-container)]
                       (is (fn? handler)))))

(deftest make-app-handler-responds-to-requests
         (testing "make-app handler returns a valid Ring response for GET /api/v1/items"
                  (let [app-container (container/make-container (test-settings))
                        handler       (main/make-app app-container)
                        response      (handler {:request-method :get
                                                :uri            "/api/v1/items"
                                                :headers        {"accept" "application/json"}
                                                :query-string   ""})]
                       (is (integer? (:status response))))))
