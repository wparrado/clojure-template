(ns clojure-template.unit.domain.domain-errors-test
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.domain.exceptions.domain-errors :as errors]))

(deftest not-found-error-has-correct-type
         (testing "not-found-error creates ex-info with ::not-found type"
                  (let [e (errors/not-found-error "item missing" {:id "123"})]
                       (is (instance? clojure.lang.ExceptionInfo e))
                       (is (= ::errors/not-found (-> e ex-data :type)))
                       (is (= "item missing" (ex-message e)))
                       (is (= "123" (-> e ex-data :id))))))

(deftest validation-error-has-correct-type
         (testing "validation-error creates ex-info with ::validation-error type"
                  (let [e (errors/validation-error "invalid field" {:field :name})]
                       (is (= ::errors/validation-error (-> e ex-data :type)))
                       (is (= "invalid field" (ex-message e))))))

(deftest conflict-error-has-correct-type
         (testing "conflict-error creates ex-info with ::conflict-error type"
                  (let [e (errors/conflict-error "duplicate" {:field :slug})]
                       (is (= ::errors/conflict-error (-> e ex-data :type))))))

(deftest not-found?-predicate
         (testing "not-found? returns true for not-found errors"
                  (is (errors/not-found? (errors/not-found-error "x" {}))))
         (testing "not-found? returns false for other error types"
                  (is (not (errors/not-found? (errors/validation-error "x" {}))))))

(deftest validation-error?-predicate
         (testing "validation-error? returns true for validation errors"
                  (is (errors/validation-error? (errors/validation-error "x" {}))))
         (testing "validation-error? returns false for other error types"
                  (is (not (errors/validation-error? (errors/not-found-error "x" {}))))))

(deftest conflict-error?-predicate
         (testing "conflict-error? returns true for conflict errors"
                  (is (errors/conflict-error? (errors/conflict-error "x" {}))))
         (testing "conflict-error? returns false for other error types"
                  (is (not (errors/conflict-error? (errors/not-found-error "x" {}))))))

(deftest error-type-returns-type-key
         (testing "error-type extracts the :type keyword from ex-data"
                  (is (= ::errors/not-found (errors/error-type (errors/not-found-error "x" {}))))
                  (is (= ::errors/validation-error (errors/error-type (errors/validation-error "x" {}))))
                  (is (= ::errors/conflict-error (errors/error-type (errors/conflict-error "x" {}))))))

(deftest error-type-returns-nil-for-plain-exception
         (testing "error-type returns nil for a plain (non ex-info) exception"
                  (is (nil? (errors/error-type (RuntimeException. "plain"))))))

(deftest domain-error-base-constructor
         (testing "domain-error creates ex-info with ::domain-error type"
                  (let [e (errors/domain-error "base error" {:detail "x"})]
                       (is (instance? clojure.lang.ExceptionInfo e))
                       (is (= ::errors/domain-error (-> e ex-data :type)))
                       (is (= "base error" (ex-message e)))
                       (is (= "x" (-> e ex-data :detail))))))
