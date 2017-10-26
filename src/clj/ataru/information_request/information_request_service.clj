(ns ataru.information-request.information-request-service
  (:require [ataru.db.db :as db]
            [ataru.log.audit-log :as audit-log]
            [ataru.util :as u]
            [camel-snake-kebab.core :as c]
            [camel-snake-kebab.extras :as t]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :as sql]))

(sql/defqueries "sql/information-request-queries.sql")

(def ^:private ->kebab-case-kw (partial t/transform-keys c/->kebab-case-keyword))

(defn store [information-request
             application-key
             session]
  {:pre [(-> information-request :subject u/not-blank?)
         (-> information-request :message u/not-blank?)
         (u/not-blank? application-key)]}
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (audit-log/log {:new       information-request
                    :operation audit-log/operation-new
                    :id        (-> session :identity :username)})
    (-> (yesql-add-information-request<! (assoc information-request :application_key application-key)
                                         {:connection conn})
        (->kebab-case-kw)
        (dissoc :id))))
