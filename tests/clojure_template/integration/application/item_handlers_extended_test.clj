(ns clojure-template.integration.application.item-handlers-extended-test
    "Extended integration tests for command/query handlers using in-memory adapters."
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.application.handlers.command-handlers :as cmds]
              [clojure-template.application.handlers.query-handlers :as qrys]
              [clojure-template.application.commands.item-commands :as cmd-t]
              [clojure-template.application.queries.item-queries :as qry-t]
              [clojure-template.application.result :as result]
              [clojure-template.domain.events.base :as base-event]
              [clojure-template.infrastructure.persistence.in-memory.item-repository :as mem-repo]
              [clojure-template.infrastructure.events.in-process-publisher :as pub]
              [clojure-template.infrastructure.clock.system-clock :as clk]
              [clojure-template.infrastructure.di.container :as di]))

(defn make-test-deps []
      (let [repo        (mem-repo/in-memory-item-repository)
            publisher   (pub/in-process-publisher)
            clock       (clk/system-clock)
            uow-factory (di/make-uow-factory repo publisher)]
           {:repo repo :publisher publisher :clock clock :uow-factory uow-factory}))

;; ── UpdateItemHandler ─────────────────────────────────────────────────────────

(deftest update-item-handler-success
         (testing "UpdateItemHandler returns success with updated dto values"
                  (let [{:keys [uow-factory clock]} (make-test-deps)
                        create-h (cmds/->CreateItemHandler uow-factory clock)
                        update-h (cmds/->UpdateItemHandler uow-factory clock)
                        created  (result/unwrap (cmds/handle create-h (cmd-t/->CreateItemCommand "Original" "desc" 10.0)))
                        res      (cmds/handle update-h (cmd-t/->UpdateItemCommand (:id created) "Updated" nil 20.0))]
                       (is (result/success? res))
                       (is (= "Updated" (:name (:value res))))
                       (is (== 20.0 (:price (:value res)))))))

(deftest update-item-handler-partial-update
         (testing "UpdateItemHandler updates only name when price is nil"
                  (let [{:keys [uow-factory clock]} (make-test-deps)
                        create-h (cmds/->CreateItemHandler uow-factory clock)
                        update-h (cmds/->UpdateItemHandler uow-factory clock)
                        created  (result/unwrap (cmds/handle create-h (cmd-t/->CreateItemCommand "Original" "desc" 10.0)))
                        res      (cmds/handle update-h (cmd-t/->UpdateItemCommand (:id created) "NewName" nil nil))]
                       (is (result/success? res))
                       (is (= "NewName" (:name (:value res))))
                       (is (== 10.0 (:price (:value res)))))))

;; ── GetItemHandler ────────────────────────────────────────────────────────────

(deftest get-item-handler-success
         (testing "GetItemHandler returns success with correct dto"
                  (let [{:keys [uow-factory clock repo]} (make-test-deps)
                        create-h (cmds/->CreateItemHandler uow-factory clock)
                        get-h    (qrys/->GetItemHandler repo)
                        created  (result/unwrap (cmds/handle create-h (cmd-t/->CreateItemCommand "FindMe" "d" 7.5)))
                        res      (qrys/handle get-h (qry-t/->GetItemQuery (:id created)))]
                       (is (result/success? res))
                       (is (= "FindMe" (:name (:value res))))
                       (is (= (:id created) (:id (:value res)))))))

(deftest get-item-handler-not-found
         (testing "GetItemHandler returns failure for unknown id"
                  (let [{:keys [repo]} (make-test-deps)
                        get-h (qrys/->GetItemHandler repo)
                        res   (qrys/handle get-h (qry-t/->GetItemQuery "00000000-0000-0000-0000-000000000000"))]
                       (is (result/failure? res)))))

(deftest get-item-handler-deleted-returns-not-found
         (testing "GetItemHandler returns failure for a deleted item"
                  (let [{:keys [uow-factory clock repo]} (make-test-deps)
                        create-h (cmds/->CreateItemHandler uow-factory clock)
                        delete-h (cmds/->DeleteItemHandler uow-factory clock)
                        get-h    (qrys/->GetItemHandler repo)
                        created  (result/unwrap (cmds/handle create-h (cmd-t/->CreateItemCommand "ToDelete" nil 3.0)))]
                       (result/unwrap (cmds/handle delete-h (cmd-t/->DeleteItemCommand (:id created))))
                       (let [res (qrys/handle get-h (qry-t/->GetItemQuery (:id created)))]
                            (is (result/failure? res))))))

;; ── SearchItemsHandler ────────────────────────────────────────────────────────

(deftest search-items-handler-by-name
         (testing "SearchItemsHandler filters items by name substring"
                  (let [{:keys [uow-factory clock repo]} (make-test-deps)
                        create-h (cmds/->CreateItemHandler uow-factory clock)
                        search-h (qrys/->SearchItemsHandler repo)]
                       (cmds/handle create-h (cmd-t/->CreateItemCommand "Apple Juice" nil 2.0))
                       (cmds/handle create-h (cmd-t/->CreateItemCommand "Orange Soda" nil 3.0))
                       (let [res (qrys/handle search-h (qry-t/->SearchItemsQuery "Apple" nil nil 10 0))]
                            (is (result/success? res))
                            (is (= 1 (count (:data (:value res)))))
                            (is (= "Apple Juice" (:name (first (:data (:value res))))))))))

(deftest search-items-handler-by-price-range
         (testing "SearchItemsHandler filters items by price range"
                  (let [{:keys [uow-factory clock repo]} (make-test-deps)
                        create-h (cmds/->CreateItemHandler uow-factory clock)
                        search-h (qrys/->SearchItemsHandler repo)]
                       (cmds/handle create-h (cmd-t/->CreateItemCommand "Cheap" nil 5.0))
                       (cmds/handle create-h (cmd-t/->CreateItemCommand "Mid" nil 15.0))
                       (cmds/handle create-h (cmd-t/->CreateItemCommand "Expensive" nil 25.0))
                       (let [res (qrys/handle search-h (qry-t/->SearchItemsQuery nil 10.0 20.0 10 0))]
                            (is (result/success? res))
                            (is (= 1 (count (:data (:value res)))))
                            (is (= "Mid" (:name (first (:data (:value res))))))))))

(deftest search-items-handler-no-filters
         (testing "SearchItemsHandler returns all active items when no filters applied"
                  (let [{:keys [uow-factory clock repo]} (make-test-deps)
                        create-h (cmds/->CreateItemHandler uow-factory clock)
                        search-h (qrys/->SearchItemsHandler repo)]
                       (cmds/handle create-h (cmd-t/->CreateItemCommand "Item 1" nil 1.0))
                       (cmds/handle create-h (cmd-t/->CreateItemCommand "Item 2" nil 2.0))
                       (cmds/handle create-h (cmd-t/->CreateItemCommand "Item 3" nil 3.0))
                       (let [res (qrys/handle search-h (qry-t/->SearchItemsQuery nil nil nil 10 0))]
                            (is (result/success? res))
                            (is (= 3 (count (:data (:value res)))))))))

;; ── Event publication ─────────────────────────────────────────────────────────

(deftest create-item-publishes-event
         (testing "CreateItemHandler publishes an ItemCreated domain event"
                  (let [{:keys [uow-factory clock publisher]} (make-test-deps)
                        captured (atom [])
                        _        (pub/subscribe! publisher
                                                 :clojure-template.domain.model.catalog.item-events/item-created
                                                 #(swap! captured conj %))
                        create-h (cmds/->CreateItemHandler uow-factory clock)]
                       (cmds/handle create-h (cmd-t/->CreateItemCommand "EventItem" nil 9.99))
                       (is (= 1 (count @captured)))
                       (is (= :clojure-template.domain.model.catalog.item-events/item-created
                              (base-event/event-type (first @captured)))))))

(deftest delete-item-publishes-event
         (testing "DeleteItemHandler publishes an ItemDeleted domain event"
                  (let [{:keys [uow-factory clock publisher]} (make-test-deps)
                        captured (atom [])
                        _        (pub/subscribe! publisher
                                                 :clojure-template.domain.model.catalog.item-events/item-deleted
                                                 #(swap! captured conj %))
                        create-h (cmds/->CreateItemHandler uow-factory clock)
                        delete-h (cmds/->DeleteItemHandler uow-factory clock)
                        created  (result/unwrap (cmds/handle create-h (cmd-t/->CreateItemCommand "ToDelete" nil 1.0)))]
                       (cmds/handle delete-h (cmd-t/->DeleteItemCommand (:id created)))
                       (is (= 1 (count @captured)))
                       (is (= :clojure-template.domain.model.catalog.item-events/item-deleted
                              (base-event/event-type (first @captured)))))))
