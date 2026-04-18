(ns clojure-template.unit.infrastructure.circuit-breaker-test
    "Unit tests for SimpleCircuitBreaker: all state transitions."
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.infrastructure.resilience.circuit-breaker :as cb]
              [clojure-template.application.ports.circuit-breaker :as port]))

;; ── Constructor ───────────────────────────────────────────────────────────────

(deftest default-arity-creates-closed-breaker
         (testing "no-arg constructor creates a closed circuit breaker"
                  (let [breaker (cb/simple-circuit-breaker)]
                       (is (not (port/open? breaker))))))

(deftest two-arg-arity-creates-closed-breaker
         (testing "two-arg constructor creates a closed circuit breaker"
                  (let [breaker (cb/simple-circuit-breaker 3 5000)]
                       (is (not (port/open? breaker))))))

;; ── Closed state ─────────────────────────────────────────────────────────────

(deftest closed-state-executes-fn-and-returns-result
         (testing "closed breaker executes fn and returns its result"
                  (let [breaker (cb/simple-circuit-breaker 3 1000)]
                       (is (= 42 (port/call breaker (fn [] 42)))))))

(deftest closed-state-resets-failure-count-on-success
         (testing "a successful call after a failure resets the failure counter"
                  (let [breaker (cb/simple-circuit-breaker 3 60000)]
                       (try (port/call breaker (fn [] (throw (RuntimeException. "fail")))) (catch Exception _))
                       (port/call breaker (fn [] :ok))
                       (is (not (port/open? breaker))))))

(deftest closed-state-opens-after-threshold
         (testing "circuit opens after failure-threshold consecutive failures"
                  (let [breaker (cb/simple-circuit-breaker 2 60000)]
                       (dotimes [_ 2]
                                (try (port/call breaker (fn [] (throw (RuntimeException. "fail"))))
                                     (catch Exception _)))
                       (is (port/open? breaker)))))

(deftest closed-state-rethrows-exception
         (testing "closed breaker rethrows the exception thrown by fn"
                  (let [breaker (cb/simple-circuit-breaker 3 60000)]
                       (is (thrown? RuntimeException
                                    (port/call breaker (fn [] (throw (RuntimeException. "boom")))))))))

;; ── Open state ───────────────────────────────────────────────────────────────

(deftest open-state-throws-without-executing-fn
         (testing "open breaker throws :circuit-open without calling fn"
                  (let [breaker (cb/simple-circuit-breaker 1 60000)
                        called? (atom false)]
                       (try (port/call breaker (fn [] (throw (RuntimeException. "fail")))) (catch Exception _))
                       (is (port/open? breaker))
                       (is (thrown? clojure.lang.ExceptionInfo
                                    (port/call breaker (fn [] (reset! called? true)))))
                       (is (not @called?)))))

(deftest open-state-transitions-to-half-open-after-timeout
         (testing "open breaker allows one attempt after reset-timeout-ms has elapsed"
                  (let [breaker (cb/simple-circuit-breaker 1 -1)]
                       (try (port/call breaker (fn [] (throw (RuntimeException. "fail")))) (catch Exception _))
                       (is (= :recovered (port/call breaker (fn [] :recovered)))))))

(deftest open-state-after-timeout-success-closes-circuit
         (testing "successful call after timeout closes the circuit"
                  (let [breaker (cb/simple-circuit-breaker 1 -1)]
                       (try (port/call breaker (fn [] (throw (RuntimeException. "fail")))) (catch Exception _))
                       (port/call breaker (fn [] :ok))
                       (is (not (port/open? breaker))))))

(deftest open-state-after-timeout-failure-reopens-circuit
         (testing "failed call in half-open state re-opens the circuit"
                  (let [breaker (cb/simple-circuit-breaker 1 -1)]
                       (try (port/call breaker (fn [] (throw (RuntimeException. "fail1")))) (catch Exception _))
                       (try (port/call breaker (fn [] (throw (RuntimeException. "fail2")))) (catch Exception _))
                       (is (port/open? breaker)))))

;; ── Half-open state ──────────────────────────────────────────────────────────

(deftest half-open-success-closes-circuit
         (testing "successful call when half-open closes the circuit"
                  (let [breaker (cb/simple-circuit-breaker 1 -1)]
                       (try (port/call breaker (fn [] (throw (RuntimeException. "fail")))) (catch Exception _))
                       (is (= :ok (port/call breaker (fn [] :ok))))
                       (is (not (port/open? breaker))))))

(deftest half-open-failure-reopens-circuit
         (testing "failed call when half-open re-opens the circuit"
                  (let [breaker (cb/simple-circuit-breaker 1 -1)]
                       (try (port/call breaker (fn [] (throw (RuntimeException. "fail1")))) (catch Exception _))
                       (try (port/call breaker (fn [] (throw (RuntimeException. "fail2")))) (catch Exception _))
                       (is (port/open? breaker)))))

;; ── Reset ─────────────────────────────────────────────────────────────────────

(deftest reset-closes-open-circuit
         (testing "reset transitions an open circuit back to closed"
                  (let [breaker (cb/simple-circuit-breaker 1 60000)]
                       (try (port/call breaker (fn [] (throw (RuntimeException. "fail")))) (catch Exception _))
                       (is (port/open? breaker))
                       (port/reset breaker)
                       (is (not (port/open? breaker))))))

(deftest reset-allows-calls-after-open
         (testing "after reset, calls succeed normally"
                  (let [breaker (cb/simple-circuit-breaker 1 60000)]
                       (try (port/call breaker (fn [] (throw (RuntimeException. "fail")))) (catch Exception _))
                       (port/reset breaker)
                       (is (= :ok (port/call breaker (fn [] :ok)))))))
