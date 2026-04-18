(ns clojure-template.integration.application.item-handlers-test
    "Integration tests for command/query handlers using in-memory adapters.
   These tests cross the application/infrastructure boundary intentionally,
   verifying that handlers work end-to-end with real adapters."
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.application.handlers.command-handlers :as cmds]
              [clojure-template.application.handlers.query-handlers :as qrys]
              [clojure-template.application.commands.item-commands :as cmd-t]
              [clojure-template.application.queries.item-queries :as qry-t]
              [clojure-template.application.result :as result]
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

(deftest create-item-handler-success
         (testing "CreateItemHandler returns ok with ItemDto on valid command"
                  (let [{:keys [uow-factory clock]} (make-test-deps)
                        handler (cmds/->CreateItemHandler uow-factory clock)
                        cmd     (cmd-t/->CreateItemCommand "Test Item" "A test" 5.99)
                        res     (cmds/handle handler cmd)]
                       (is (result/success? res))
                       (is (= "Test Item" (:name (:value res)))))))

(deftest create-item-handler-invalid-price
         (testing "CreateItemHandler returns failure on negative price"
                  (let [{:keys [uow-factory clock]} (make-test-deps)
                        handler (cmds/->CreateItemHandler uow-factory clock)
                        cmd     (cmd-t/->CreateItemCommand "Bad Item" nil -1)
                        res     (cmds/handle handler cmd)]
                       (is (result/failure? res)))))

(deftest update-item-handler-not-found
         (testing "UpdateItemHandler returns failure when item does not exist"
                  (let [{:keys [uow-factory clock]} (make-test-deps)
                        handler (cmds/->UpdateItemHandler uow-factory clock)
                        cmd     (cmd-t/->UpdateItemCommand "00000000-0000-0000-0000-000000000000" "New" nil nil)
                        res     (cmds/handle handler cmd)]
                       (is (result/failure? res)))))

(deftest delete-item-handler-success
         (testing "DeleteItemHandler returns ok true on valid delete"
                  (let [{:keys [uow-factory clock]} (make-test-deps)
                        create-h (cmds/->CreateItemHandler uow-factory clock)
                        delete-h (cmds/->DeleteItemHandler uow-factory clock)
                        created  (result/unwrap (cmds/handle create-h (cmd-t/->CreateItemCommand "ToDelete" nil 1.0)))
                        res      (cmds/handle delete-h (cmd-t/->DeleteItemCommand (:id created)))]
                       (is (result/success? res))
                       (is (true? (:value res))))))

(deftest list-items-handler-returns-paginated
         (testing "ListItemsHandler returns paginated result"
                  (let [{:keys [uow-factory clock repo]} (make-test-deps)
                        create-h (cmds/->CreateItemHandler uow-factory clock)
                        list-h   (qrys/->ListItemsHandler repo)]
                       (cmds/handle create-h (cmd-t/->CreateItemCommand "Item A" nil 1.0))
                       (cmds/handle create-h (cmd-t/->CreateItemCommand "Item B" nil 2.0))
                       (let [res (qrys/handle list-h (qry-t/->ListItemsQuery 10 0))]
                            (is (result/success? res))
                            (is (= 2 (:total (:value res))))))))

(deftest delete-item-handler-not-found
         (testing "DeleteItemHandler returns failure when item does not exist"
                  (let [{:keys [uow-factory clock]} (make-test-deps)
                        delete-h (cmds/->DeleteItemHandler uow-factory clock)
                        res      (cmds/handle delete-h (cmd-t/->DeleteItemCommand "00000000-0000-0000-0000-000000000000"))]
                       (is (result/failure? res)))))

(deftest list-items-uses-default-limit-and-offset
         (testing "ListItemsHandler works with nil limit and offset (uses defaults)"
                  (let [{:keys [uow-factory clock repo]} (make-test-deps)
                        create-h (cmds/->CreateItemHandler uow-factory clock)
                        list-h   (qrys/->ListItemsHandler repo)]
                       (cmds/handle create-h (cmd-t/->CreateItemCommand "Item X" nil 5.0))
                       (let [res (qrys/handle list-h (qry-t/->ListItemsQuery nil nil))]
                            (is (result/success? res))
                            (is (= 1 (:total (:value res))))))))
