(ns clojure-template.integration.infrastructure.item-repository-contract-test
    "Contract tests that every IItemRepository implementation must satisfy."
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.domain.model.catalog.item :as item-agg]
              [clojure-template.domain.ports.outbound.item-repository :as repo-port]
              [clojure-template.domain.specifications.item-specifications :as specs]
              [clojure-template.infrastructure.persistence.in-memory.item-repository :as mem-repo])
    (:import [java.time Instant]))

(defn make-item [name price]
      (item-agg/create-item {:name name :price price :now (Instant/now)}))

(defn run-contract-tests [make-repo]
      (testing "save and find-by-id"
               (let [repo (make-repo)
                     item (make-item "Widget" 5.99)]
                    (repo-port/save repo item)
                    (let [found (repo-port/find-by-id repo (:id item))]
                         (is (some? found))
                         (is (= (:id item) (:id found))))))

      (testing "find-by-id returns nil for unknown id"
               (let [repo (make-repo)]
                    (is (nil? (repo-port/find-by-id repo "00000000-0000-0000-0000-000000000000")))))

      (testing "find-all paginates"
               (let [repo (make-repo)
                     a    (make-item "A" 1)
                     b    (make-item "B" 2)
                     c    (make-item "C" 3)]
                    (repo-port/save repo a)
                    (repo-port/save repo b)
                    (repo-port/save repo c)
                    (let [{:keys [items total]} (repo-port/find-all repo 2 0)]
                         (is (= 3 total))
                         (is (= 2 (count items))))))

      (testing "count-all excludes deleted"
               (let [repo (make-repo)
                     item (make-item "Deletable" 1)
                     del  (item-agg/delete-item item {:now (Instant/now)})]
                    (repo-port/save repo del)
                    (is (= 0 (repo-port/count-all repo)))))

      (testing "find-matching with ActiveItemSpec"
               (let [repo   (make-repo)
                     active (make-item "Active" 1)
                     dead   (item-agg/delete-item (make-item "Dead" 2) {:now (Instant/now)})
                     spec   (specs/active-item-spec)]
                    (repo-port/save repo active)
                    (repo-port/save repo dead)
                    (let [{:keys [items]} (repo-port/find-matching repo spec 10 0)]
                         (is (= 1 (count items)))
                         (is (= (:id active) (:id (first items))))))))

(deftest in-memory-repository-contract
         (run-contract-tests mem-repo/in-memory-item-repository))
