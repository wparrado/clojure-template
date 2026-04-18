(ns clojure-template.unit.presentation.error-handlers-test
    "Unit tests for the presentation error-handler layer."
    (:require [clojure.test :refer [deftest is]]
              [clojure-template.presentation.error-handlers :as sut]
              [clojure-template.application.exceptions :as errors]))

(deftest not-found-error-returns-404
         (let [e   (errors/not-found-error "Item not found" {:id "abc"})
               res (sut/error->response e)]
              (is (= 404 (:status res)))))

(deftest validation-error-returns-422
         (let [e   (errors/validation-error "Invalid input" {:field :name})
               res (sut/error->response e)]
              (is (= 422 (:status res)))))

(deftest conflict-error-returns-409
         (let [e   (errors/conflict-error "Name already exists" {:name "Widget"})
               res (sut/error->response e)]
              (is (= 409 (:status res)))))

(deftest unknown-error-returns-500
         (let [e   (ex-info "Something broke" {:type :some/other-type})
               res (sut/error->response e)]
              (is (= 500 (:status res)))))

(deftest error-response-contains-message
         (let [e   (errors/not-found-error "Item not found" {})
               res (sut/error->response e)]
              (is (= "Item not found" (get-in res [:body :error])))))

(deftest error-response-contains-type
         (let [e   (errors/not-found-error "Item not found" {})
               res (sut/error->response e)]
              (is (= "not-found" (get-in res [:body :type])))))

(deftest error-response-contains-details
         (let [e   (errors/not-found-error "Item not found" {:id "abc" :entity "item"})
               res (sut/error->response e)]
              (is (= {:id "abc" :entity "item"} (get-in res [:body :details])))))
