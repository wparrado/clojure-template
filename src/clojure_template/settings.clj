(ns clojure-template.settings
    "Typed configuration loaded via aero from resources/config.edn."
    (:require [aero.core :as aero]
              [clojure.java.io :as io]))

(defn load-settings
      "Reads config from resources/config.edn (aero), returns a settings map."
      []
      (let [{:keys [port env log-level]} (aero/read-config (io/resource "config.edn"))]
           {:port      (Integer/parseInt (str port))
            :env       (keyword env)
            :log-level (keyword log-level)}))
