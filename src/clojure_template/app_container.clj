(ns clojure-template.app-container
    "Composition root — wires all adapters to ports."
    (:require [clojure-template.infrastructure.di.container :as infra]
              [clojure-template.infrastructure.clock.system-clock :as clk]
              [clojure-template.application.handlers.command-handlers :as cmds]
              [clojure-template.application.handlers.query-handlers :as qrys]
              [clojure-template.application.services.item-service :as svc]))

(defrecord AppContainer [settings persistence events resilience clock app-service])

(defn make-container
      "Builds and returns the fully wired application container."
      [settings]
      (let [persistence (infra/make-persistence-container)
            events-c    (infra/make-events-container)
            resilience  (infra/make-resilience-container)
            clock       (clk/system-clock)
            repo        (:item-repository persistence)
            pub         (:event-publisher events-c)
            uow-factory (infra/make-uow-factory repo pub)
            create-h    (cmds/->CreateItemHandler uow-factory clock)
            update-h    (cmds/->UpdateItemHandler uow-factory clock)
            delete-h    (cmds/->DeleteItemHandler uow-factory clock)
            get-h       (qrys/->GetItemHandler repo)
            list-h      (qrys/->ListItemsHandler repo)
            search-h    (qrys/->SearchItemsHandler repo)
            app-svc     (svc/->ItemApplicationService create-h update-h delete-h
                                                      get-h list-h search-h)]
           (->AppContainer settings persistence events-c resilience clock app-svc)))
