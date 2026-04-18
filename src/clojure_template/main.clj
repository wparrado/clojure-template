(ns clojure-template.main
    "Application entry point."
    (:require [clojure-template.settings :as settings]
              [clojure-template.app-container :as container]
              [clojure-template.presentation.api.v1.routers.item-router :as router]
              [clojure-template.presentation.middlewares.correlation-id :as mid]
              [reitit.ring :as reitit-ring]
              [reitit.coercion.malli :as malli-coercion]
              [reitit.ring.coercion :as ring-coercion]
              [reitit.ring.middleware.muuntaja :as muuntaja-mw]
              [ring.adapter.jetty :as jetty]
              [taoensso.timbre :as log])
    (:gen-class))

(defn make-app [app-container]
      (let [svc    (:app-service app-container)
            routes (router/item-routes svc)]
           (-> (reitit-ring/ring-handler
                (reitit-ring/router
                 routes
                 {:data      {:coercion   malli-coercion/coercion
                              :middleware [muuntaja-mw/format-middleware
                                           ring-coercion/coerce-exceptions-middleware
                                           ring-coercion/coerce-request-middleware
                                           ring-coercion/coerce-response-middleware]}
                  :conflicts nil})
                (reitit-ring/create-default-handler))
               mid/wrap-correlation-id)))

(defn -main [& _args]
      (let [cfg    (settings/load-settings)
            _      (log/merge-config! {:min-level (:log-level cfg)})
            app    (container/make-container cfg)
            ring   (make-app app)
            server (jetty/run-jetty ring {:port (:port cfg) :join? false})]
           (log/info "Starting server on port" (:port cfg))
           (.addShutdownHook (Runtime/getRuntime)
                             (Thread. ^Runnable
                              (fn []
                                  (log/info "Shutting down server...")
                                  (.stop server)
                                  (log/info "Server stopped."))))
           (.join server)))
