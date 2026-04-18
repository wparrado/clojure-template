(ns clojure-template.application.ports.unit-of-work
    "IUnitOfWork port — groups repository ops and event dispatch.")

(defprotocol IUnitOfWork
  "Transactional boundary for repository operations and event publishing."
  (get-item-repository [this] "Returns the IItemRepository within this unit of work.")
  (commit [this] "Commits the unit of work: persists changes and publishes collected events.")
  (rollback [this] "Rolls back all changes in this unit of work."))
