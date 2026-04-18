(ns clojure-template.unit.domain.value-objects-test
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.domain.model.catalog.value-objects :as vo])
    (:import [java.util UUID]))

;; ── ItemDescription ───────────────────────────────────────────────────────────

(deftest item-description-accepts-nil
         (testing "->item-description accepts nil (optional field)"
                  (let [d (vo/->item-description nil)]
                       (is (instance? clojure_template.domain.model.catalog.value_objects.ItemDescription d))
                       (is (nil? (:value d))))))

(deftest item-description-accepts-valid-string
         (testing "->item-description accepts a non-empty string"
                  (let [d (vo/->item-description "A fine item")]
                       (is (= "A fine item" (:value d))))))

(deftest item-description-rejects-too-long-string
         (testing "->item-description rejects strings longer than 1000 chars"
                  (is (thrown? clojure.lang.ExceptionInfo
                               (vo/->item-description (apply str (repeat 1001 "x")))))))

;; ── ItemId ────────────────────────────────────────────────────────────────────

(deftest item-id-accepts-valid-uuid-string
         (testing "->item-id accepts a well-formed UUID string"
                  (let [uuid-str (str (UUID/randomUUID))
                        id       (vo/->item-id uuid-str)]
                       (is (instance? clojure_template.domain.model.catalog.value_objects.ItemId id))
                       (is (= uuid-str (:value id))))))

(deftest item-id-rejects-nil
         (testing "->item-id rejects nil"
                  (is (thrown? clojure.lang.ExceptionInfo
                               (vo/->item-id nil)))))

(deftest item-id-rejects-non-uuid-string
         (testing "->item-id rejects an arbitrary string that is not a UUID"
                  (is (thrown? clojure.lang.ExceptionInfo
                               (vo/->item-id "not-a-uuid")))))

(deftest new-item-id-generates-valid-uuid
         (testing "new-item-id returns an ItemId whose value parses as UUID"
                  (let [id (vo/new-item-id)]
                       (is (instance? clojure_template.domain.model.catalog.value_objects.ItemId id))
                       (is (some? (UUID/fromString (:value id)))))))
