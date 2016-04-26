(ns lomake-editori.core
  (:require [taoensso.timbre :refer [info]]
            [lomake-editori.clerk-routes :refer [clerk-routes]]
            [lomake-editori.db.migrations :as migrations]
            [clojure.core.async :as a]
            [environ.core :refer [env]]
            [ring.middleware.reload :refer [wrap-reload]]
            [cider.nrepl :refer [cider-nrepl-handler]]
            [clojure.tools.nrepl.server :refer [start-server]]
            [aleph.http :as http])
  (:gen-class))

(def server (atom nil))

(defn start-repl! []
  (when (:dev? env)
    (do
      (start-server :port 3333 :handler cider-nrepl-handler)
      (info "nREPL started on port 3333"))))

(defn run-migrations []
  ;; Only run migrations when in dev mode for now
  ;; Deployment has to catch up before we can run migrations on test/prod
  ;; servers
  (migrations/migrate :db "db.migration"))

(defn- try-f [f] (try (f) (catch Exception ignored nil)))

(defn wait-forever [] @(promise))

(defn -main [& [prt & _]]
  (let [port (or (try-f (fn [] (Integer/parseInt prt)))
                 8350)]
    (do
      (a/go (start-repl!))
      (a/go (run-migrations))
      (info "Starting server on port" port)
      (reset! server
              (http/start-server
               (if (:dev? env)
                 (wrap-reload (var clerk-routes))
                 clerk-routes)
               {:port port}))
      (info "Started server on port" port)
      (wait-forever))))
