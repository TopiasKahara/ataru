(ns ataru.middleware.session-client
  (:require [ataru.config.core :refer [config]]
            [ring.util.http-response :as response]))

(defn wrap-session-client-headers [handler]
  [handler]
  (fn [{:keys [headers] :as req}]
      (let [user-agent (:user-agent headers)
            client-ip  (or (get headers "x-real-ip")
                           (get headers "x-forwarded-for"))]
        (handler (-> req
                     (assoc-in [:session :user-agent] user-agent)
                     (assoc-in [:session :client-ip] client-ip))))))