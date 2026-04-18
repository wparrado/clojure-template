(ns clojure-template.unit.infrastructure.clock-test
    "Unit tests for SystemClock and FakeClock."
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.infrastructure.clock.system-clock :as clk]
              [clojure-template.domain.ports.inbound.clock :as clock-port])
    (:import [java.time Instant]))

(deftest system-clock-returns-instant
         (testing "SystemClock.now returns a java.time.Instant"
                  (let [clock (clk/system-clock)]
                       (is (instance? Instant (clock-port/now clock))))))

(deftest fake-clock-returns-fixed-time
         (testing "FakeClock.now returns the fixed Instant it was initialised with"
                  (let [fixed (Instant/parse "2024-06-01T12:00:00Z")
                        clock (clk/fake-clock fixed)]
                       (is (= fixed (clock-port/now clock))))))

(deftest fake-clock-is-deterministic
         (testing "FakeClock.now always returns the same value when not advanced"
                  (let [fixed (Instant/parse "2024-06-01T12:00:00Z")
                        clock (clk/fake-clock fixed)]
                       (is (= (clock-port/now clock) (clock-port/now clock))))))

(deftest advance-fake-clock-adds-milliseconds
         (testing "advance-fake-clock! advances the clock by given milliseconds"
                  (let [fixed (Instant/parse "2024-01-01T00:00:00Z")
                        clock (clk/fake-clock fixed)]
                       (clk/advance-fake-clock! clock 1000)
                       (is (= (Instant/parse "2024-01-01T00:00:01Z") (clock-port/now clock))))))

(deftest advance-fake-clock-accumulates
         (testing "multiple advances accumulate correctly"
                  (let [fixed (Instant/parse "2024-01-01T00:00:00Z")
                        clock (clk/fake-clock fixed)]
                       (clk/advance-fake-clock! clock 500)
                       (clk/advance-fake-clock! clock 500)
                       (is (= (Instant/parse "2024-01-01T00:00:01Z") (clock-port/now clock))))))
