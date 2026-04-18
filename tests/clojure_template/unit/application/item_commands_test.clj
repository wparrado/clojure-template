(ns clojure-template.unit.application.item-commands-test
    "Unit tests for item command factory functions."
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.application.commands.item-commands :as cmds]))

;; ── CreateItemCommand ─────────────────────────────────────────────────────────

(deftest create-item-command-has-correct-type
         (testing "->CreateItemCommand produces a map with :type :create-item"
                  (let [cmd (cmds/->CreateItemCommand "Widget" "desc" 9.99)]
                       (is (= :create-item (:type cmd))))))

(deftest create-item-command-preserves-fields
         (testing "->CreateItemCommand preserves name, description and price"
                  (let [cmd (cmds/->CreateItemCommand "Widget" "A widget" 5.50)]
                       (is (= "Widget" (:name cmd)))
                       (is (= "A widget" (:description cmd)))
                       (is (= 5.50 (:price cmd))))))

(deftest create-item-command-accepts-nil-description
         (testing "->CreateItemCommand accepts nil description"
                  (let [cmd (cmds/->CreateItemCommand "Widget" nil 1.0)]
                       (is (nil? (:description cmd))))))

;; ── UpdateItemCommand ─────────────────────────────────────────────────────────

(deftest update-item-command-has-correct-type
         (testing "->UpdateItemCommand produces a map with :type :update-item"
                  (let [cmd (cmds/->UpdateItemCommand "id-1" "New" nil nil)]
                       (is (= :update-item (:type cmd))))))

(deftest update-item-command-includes-all-non-nil-fields
         (testing "->UpdateItemCommand includes name, description and price when all non-nil"
                  (let [cmd (cmds/->UpdateItemCommand "id-1" "Name" "Desc" 7.0)]
                       (is (contains? cmd :name))
                       (is (contains? cmd :description))
                       (is (contains? cmd :price))
                       (is (= "Name" (:name cmd)))
                       (is (= "Desc" (:description cmd)))
                       (is (= 7.0 (:price cmd))))))

(deftest update-item-command-excludes-nil-fields
         (testing "->UpdateItemCommand omits nil name, description, and price"
                  (let [cmd (cmds/->UpdateItemCommand "id-1" nil nil nil)]
                       (is (not (contains? cmd :name)))
                       (is (not (contains? cmd :description)))
                       (is (not (contains? cmd :price))))))

(deftest update-item-command-includes-only-name
         (testing "->UpdateItemCommand includes only :name when others are nil"
                  (let [cmd (cmds/->UpdateItemCommand "id-1" "OnlyName" nil nil)]
                       (is (contains? cmd :name))
                       (is (not (contains? cmd :description)))
                       (is (not (contains? cmd :price))))))

(deftest update-item-command-includes-only-price
         (testing "->UpdateItemCommand includes only :price when others are nil"
                  (let [cmd (cmds/->UpdateItemCommand "id-1" nil nil 99.0)]
                       (is (not (contains? cmd :name)))
                       (is (not (contains? cmd :description)))
                       (is (contains? cmd :price)))))

(deftest update-item-command-includes-only-description
         (testing "->UpdateItemCommand includes only :description when others are nil"
                  (let [cmd (cmds/->UpdateItemCommand "id-1" nil "Only desc" nil)]
                       (is (not (contains? cmd :name)))
                       (is (contains? cmd :description))
                       (is (not (contains? cmd :price))))))

;; ── DeleteItemCommand ─────────────────────────────────────────────────────────

(deftest delete-item-command-has-correct-type
         (testing "->DeleteItemCommand produces a map with :type :delete-item"
                  (let [cmd (cmds/->DeleteItemCommand "id-1")]
                       (is (= :delete-item (:type cmd))))))

(deftest delete-item-command-preserves-id
         (testing "->DeleteItemCommand preserves the item id"
                  (let [cmd (cmds/->DeleteItemCommand "uuid-xyz")]
                       (is (= "uuid-xyz" (:id cmd))))))
