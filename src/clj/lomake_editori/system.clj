(ns lomake-editori.system
  (:require [com.stuartsierra.component :as component]
            [lomake-editori.core :as server]
            [taoensso.timbre :refer [info]]))

(defn new-system
  []
  (component/system-map
    :server (server/new-server)))

(defn ^:private wait-forever
  []
  @(promise))

(defn -main [& _]
  (let [system (new-system)]
    (info "Starting lomake-editori system")
    (let [_ (component/start-system system)]
      (wait-forever))))
