(ns clojure-template.application.ports.item-application-service
    "IItemApplicationService inbound port.")

(defprotocol IItemApplicationService
  "Inbound port for all item use cases."
  (create-item [this cmd] "Handles CreateItemCommand. Returns Result<ItemDto>.")
  (update-item [this cmd] "Handles UpdateItemCommand. Returns Result<ItemDto>.")
  (delete-item [this cmd] "Handles DeleteItemCommand. Returns Result<boolean>.")
  (get-item    [this qry] "Handles GetItemQuery. Returns Result<ItemDto>.")
  (list-items  [this qry] "Handles ListItemsQuery. Returns Result<PaginatedResult<ItemDto>>.")
  (search-items [this qry] "Handles SearchItemsQuery. Returns Result<PaginatedResult<ItemDto>>."))
