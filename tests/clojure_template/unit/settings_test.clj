(ns clojure-template.unit.settings-test
    "Unit tests for settings/load-settings."
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.settings :as settings]))

(deftest load-settings-returns-map-with-required-keys
         (testing "load-settings returns a map containing :port, :env and :log-level"
                  (let [cfg (settings/load-settings)]
                       (is (map? cfg))
                       (is (contains? cfg :port))
                       (is (contains? cfg :env))
                       (is (contains? cfg :log-level)))))

(deftest load-settings-port-is-integer
         (testing "load-settings :port is a positive integer"
                  (let [cfg (settings/load-settings)]
                       (is (integer? (:port cfg)))
                       (is (pos? (:port cfg))))))

(deftest load-settings-env-is-keyword
         (testing "load-settings :env is a keyword"
                  (let [cfg (settings/load-settings)]
                       (is (keyword? (:env cfg))))))

(deftest load-settings-log-level-is-keyword
         (testing "load-settings :log-level is a keyword"
                  (let [cfg (settings/load-settings)]
                       (is (keyword? (:log-level cfg))))))

(deftest load-settings-default-port
         (testing "load-settings defaults port to 3000 when APP_PORT env var is absent"
                  (let [cfg (settings/load-settings)]
                       (is (= 3000 (:port cfg))))))
