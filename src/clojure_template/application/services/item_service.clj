(ns clojure-template.application.services.item-service
    "ItemApplicationService — implements IItemApplicationService using CQRS handlers."
    (:require [clojure-template.application.ports.item-application-service :as port]
              [clojure-template.application.handlers.command-handlers :as cmds]
              [clojure-template.application.handlers.query-handlers :as qrys]))

(defrecord ItemApplicationService [create-handler update-handler delete-handler
                                   get-handler list-handler search-handler]
  port/IItemApplicationService
  (create-item [_ cmd]
    (cmds/handle create-handler cmd))
  (update-item [_ cmd]
    (cmds/handle update-handler cmd))
  (delete-item [_ cmd]
    (cmds/handle delete-handler cmd))
  (get-item [_ qry]
    (qrys/handle get-handler qry))
  (list-items [_ qry]
    (qrys/handle list-handler qry))
  (search-items [_ qry]
    (qrys/handle search-handler qry)))
