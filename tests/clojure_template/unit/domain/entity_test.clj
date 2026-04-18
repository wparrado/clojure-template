(ns clojure-template.unit.domain.entity-test
    "Unit tests for IEntity helpers."
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.domain.model.entity :as entity]
              [clojure-template.domain.model.catalog.item :as item])
    (:import [java.time Instant]))

(def ^:private now (Instant/now))

(defn- make-item
       ([] (item/create-item {:name "Widget" :price 9.99 :now now}))
       ([id] (item/create-item {:id id :name "Widget" :price 9.99 :now now})))

(deftest entity=-same-id-returns-true
         (testing "entity= returns true for two entities with the same id"
                  (let [id "00000000-0000-0000-0000-000000000001"
                        a  (make-item id)
                        b  (make-item id)]
                       (is (entity/entity= a b)))))

(deftest entity=-different-id-returns-false
         (testing "entity= returns false for two entities with different ids"
                  (let [a (make-item)
                        b (make-item)]
                       (is (not (entity/entity= a b))))))

(deftest entity-id-returns-string-uuid
         (testing "entity-id returns the string UUID of the entity"
                  (let [id  "00000000-0000-0000-0000-000000000002"
                        itm (make-item id)]
                       (is (= id (entity/entity-id itm))))))

(deftest created-at-returns-timestamp
         (testing "created-at returns the creation instant"
                  (let [itm (make-item)]
                       (is (= now (entity/created-at itm))))))

(deftest updated-at-returns-timestamp
         (testing "updated-at returns the last-updated instant"
                  (let [itm (make-item)]
                       (is (= now (entity/updated-at itm))))))

(deftest deleted?-returns-false-for-new-item
         (testing "deleted? returns false for a freshly created item"
                  (let [itm (make-item)]
                       (is (not (entity/deleted? itm))))))

(deftest deleted?-returns-true-after-delete
         (testing "deleted? returns true after item is soft-deleted"
                  (let [itm (make-item)
                        del (item/delete-item itm {:now now})]
                       (is (entity/deleted? del)))))
