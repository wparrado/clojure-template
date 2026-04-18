(ns clojure-template.unit.application.result-test
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.application.result :as r]))

(deftest ok-creates-success
         (testing "ok wraps value in Success"
                  (let [res (r/ok 42)]
                       (is (instance? clojure_template.application.result.Success res))
                       (is (= 42 (:value res))))))

(deftest err-creates-failure
         (testing "err wraps error in Failure"
                  (let [e   (ex-info "oops" {})
                        res (r/err e)]
                       (is (instance? clojure_template.application.result.Failure res))
                       (is (= e (:error res))))))

(deftest success?-predicate
         (testing "success? returns true for Success"
                  (is (r/success? (r/ok :x))))
         (testing "success? returns false for Failure"
                  (is (not (r/success? (r/err (ex-info "e" {})))))))

(deftest failure?-predicate
         (testing "failure? returns true for Failure"
                  (is (r/failure? (r/err (ex-info "e" {})))))
         (testing "failure? returns false for Success"
                  (is (not (r/failure? (r/ok :x))))))

(deftest unwrap-success
         (testing "unwrap returns value from Success"
                  (is (= 99 (r/unwrap (r/ok 99))))))

(deftest unwrap-failure-throws
         (testing "unwrap throws the error from Failure"
                  (let [e (ex-info "boom" {:code 1})]
                       (is (thrown? clojure.lang.ExceptionInfo (r/unwrap (r/err e))))
                       (try
                        (r/unwrap (r/err e))
                        (catch clojure.lang.ExceptionInfo thrown
                               (is (= e thrown)))))))

(deftest map-ok-applies-f-to-success
         (testing "map-ok transforms value inside Success"
                  (is (= (r/ok 10) (r/map-ok (r/ok 5) #(* 2 %)))))
         (testing "map-ok leaves Failure unchanged"
                  (let [failure (r/err (ex-info "e" {}))]
                       (is (= failure (r/map-ok failure #(* 2 %)))))))

(deftest flat-map-applies-f-to-success
         (testing "flat-map chains Result-returning function on Success"
                  (is (= (r/ok 6) (r/flat-map (r/ok 3) #(r/ok (* 2 %))))))
         (testing "flat-map propagates Failure to a new Failure"
                  (let [f (r/flat-map (r/ok 3) #(r/err (ex-info (str %) {})))]
                       (is (r/failure? f))))
         (testing "flat-map leaves Failure unchanged"
                  (let [failure (r/err (ex-info "e" {}))]
                       (is (= failure (r/flat-map failure #(r/ok (* 2 %))))))))

(deftest try-result-ok-when-no-exception
         (testing "try-result returns ok when body succeeds"
                  (let [res (r/try-result (+ 1 2))]
                       (is (r/success? res))
                       (is (= 3 (:value res))))))

(deftest try-result-err-on-exception-info
         (testing "try-result catches ExceptionInfo and wraps it in Failure"
                  (let [e   (ex-info "domain err" {:type :test})
                        res (r/try-result (throw e))]
                       (is (r/failure? res))
                       (is (= e (:error res))))))

(deftest try-result-propagates-non-exception-info
         (testing "try-result does not catch plain RuntimeException"
                  (is (thrown? RuntimeException
                               (r/try-result (throw (RuntimeException. "plain")))))))
