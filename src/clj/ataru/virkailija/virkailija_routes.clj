(ns ataru.virkailija.virkailija-routes
  (:require [ataru.config.url-helper :as url-helper]
            [ataru.middleware.cache-control :as cache-control]
            [ataru.middleware.user-feedback :as user-feedback]
            [ataru.middleware.session-store :refer [create-store]]
            [ataru.middleware.session-timeout :as session-timeout]
            [ataru.schema.form-schema :as ataru-schema]
            [ataru.virkailija.authentication.auth-middleware :as auth-middleware]
            [ataru.dob :as dob]
            [ataru.virkailija.authentication.auth-routes :refer [auth-routes]]
            [ataru.virkailija.authentication.auth-utils :as auth-utils]
            [ataru.applications.permission-check :as permission-check]
            [ataru.applications.application-service :as application-service]
            [ataru.forms.form-store :as form-store]
            [ataru.files.file-store :as file-store]
            [ataru.util.client-error :as client-error]
            [ataru.applications.application-access-control :as access-controlled-application]
            [ataru.forms.form-access-control :as access-controlled-form]
            [ataru.haku.haku-service :as haku-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [ataru.information-request.information-request-service :as information-request]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.applications.excel-export :as excel]
            [ataru.virkailija.user.session-organizations :refer [organization-list]]
            [ataru.statistics.statistics-service :as statistics-service]
            [cheshire.core :as json]
            [cheshire.generate :refer [add-encoder]]
            [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [compojure.api.sweet :as api]
            [compojure.api.exception :as ex]
            [compojure.response :refer [Renderable]]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [manifold.deferred]                             ;; DO NOT REMOVE! extend-protocol below breaks otherwise!
            [ataru.config.core :refer [config]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.session :as ring-session]
            [ring.middleware.logger :refer [wrap-with-logger] :as middleware-logger]
            [ring.util.http-response :refer [ok internal-server-error not-found bad-request content-type set-cookie]]
            [ring.util.response :refer [redirect header]]
            [schema.core :as s]
            [selmer.parser :as selmer]
            [taoensso.timbre :refer [spy debug error warn info]]
            [com.stuartsierra.component :as component]
            [clout.core :as clout]
            [ring.util.http-response :as response]
            [org.httpkit.client :as http]
            [medley.core :refer [map-kv]]
            [ataru.cache.cache-service :as cache]
            [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit])
  (:import java.time.ZonedDateTime
           java.time.format.DateTimeFormatter))

;; Compojure will normally dereference deferreds and return the realized value.
;; This unfortunately blocks the thread. Since aleph can accept the un-realized
;; deferred, we extend compojure's Renderable protocol to pass the deferred
;; through unchanged so that the thread won't be blocked.
(extend-protocol Renderable
                 manifold.deferred.Deferred
                 (render [d _] d))

(def ^:private cache-fingerprint (System/currentTimeMillis))

(def client-page-patterns #"(editor|applications)")

(def client-routes
  (clout/route-compile "/:page" {:page client-page-patterns}))

(def client-sub-routes
  (clout/route-compile "/:page/*" {:page client-page-patterns}))

(add-encoder ZonedDateTime
             (fn [d json-generator]
               (.writeString
                json-generator
                (.format d DateTimeFormatter/ISO_OFFSET_DATE_TIME))))

(defn render-virkailija-page
  []
  (let [config (json/generate-string (or (:public-config config) {}))]
    (-> (selmer/render-file "templates/virkailija.html"
                            {:cache-fingerprint cache-fingerprint
                             :config            config})
        (ok)
        (content-type "text/html"))))

(api/defroutes app-routes
  (api/undocumented
    (api/GET "/" [] (render-virkailija-page))
    (api/GET client-routes [] (render-virkailija-page))
    (api/GET client-sub-routes [] (render-virkailija-page))))

(defn- render-file-in-dev
  [filename js-config]
  (if (:dev? env)
    (selmer/render-file filename {:config (json/generate-string js-config)})
    (not-found "Not found")))

(defn- wrap-database-backed-session [handler]
  (ring-session/wrap-session handler
                             {:root "/lomake-editori"
                              :cookie-attrs {:secure (not (:dev? env))}
                              :store (create-store)}))

(api/defroutes test-routes
  (api/undocumented
   (api/GET "/virkailija-test.html" []
            (if (:dev? env)
              (render-file-in-dev "templates/virkailija-test.html" {})
              (route/not-found "Not found")))
   (api/GET "/virkailija-question-group-test.html" []
     (if (:dev? env)
       (render-file-in-dev "templates/virkailija-question-group-test.html" {})
       (route/not-found "Not found")))
   (api/GET "/virkailija-question-group-application-handling-test.html" []
     (if (:dev? env)
       (render-file-in-dev "templates/virkailija-question-group-application-handling-test.html" {:form-key (form-store/get-latest-form-by-name "Kysymysryhmä: testilomake")})
       (route/not-found "Not found")))
   (api/GET "/spec/:filename.js" [filename]
            (if (:dev? env)
              (render-file-in-dev (str "spec/" filename ".js") {})
              (route/not-found "Not found")))))

(defn api-routes [{:keys [organization-service tarjonta-service virkailija-tarjonta-service cache-service]}]
    (api/context "/api" []
                 :tags ["form-api"]

                 (api/GET "/user-info" {session :session}
                          (ok {:username (-> session :identity :username)
                               :organizations (organization-list session)}))

                 (api/GET "/forms" {session :session}
                   :summary "Return forms for editor view. Also used by external services.
                             In practice this is Tarjonta system only for now.
                             Return forms authorized with editor right (:form-edit)"
                   :return {:forms [ataru-schema/Form]}
                   (ok (access-controlled-form/get-forms-for-editor session organization-service)))

                 (api/GET "/forms-in-use" {session :session}
                          :summary "Return a map of form->hakus-currently-in-use-in-tarjonta-service"
                          :return {s/Str {s/Str {:haku-oid s/Str :haku-name ataru-schema/LocalizedStringOptional}}}
                          (ok (.get-forms-in-use virkailija-tarjonta-service (-> session :identity :username))))

                 (api/GET "/forms/:id" []
                          :path-params [id :- Long]
                          :return ataru-schema/FormWithContent
                          :summary "Get content for form"
                          (ok (form-store/fetch-form id)))

                 (api/DELETE "/forms/:id" {session :session}
                   :path-params [id :- Long]
                   :summary "Mark form as deleted"
                   (ok (access-controlled-form/delete-form id session organization-service)))

                 (api/POST "/forms" {session :session}
                   :summary "Persist changed form."
                   :body [form ataru-schema/FormWithContent]
                   (ok (access-controlled-form/post-form form session organization-service)))

                 (api/POST "/client-error" []
                           :summary "Log client-side errors to server log"
                           :body [error-details client-error/ClientError]
                           (do
                             (client-error/log-client-error error-details)
                             (ok {})))

                 (api/context "/applications" []
                   :tags ["applications-api"]

                  (api/GET "/list" {session :session}
                           :query-params [{formKey      :- s/Str nil}
                                          {hakukohdeOid :- s/Str nil}
                                          {hakuOid      :- s/Str nil}
                                          {ssn          :- s/Str nil}
                                          {dob          :- s/Str nil}
                                          {email        :- s/Str nil}
                                          {name         :- s/Str nil}]
                           :summary "Return applications header-level info for form"
                           :return {:applications [ataru-schema/ApplicationInfo]}
                           (cond
                             (some? formKey)
                             (ok (application-service/get-application-list-by-form formKey session organization-service))

                             (some? hakukohdeOid)
                             (ok (access-controlled-application/get-application-list-by-hakukohde hakukohdeOid session organization-service))

                             (some? hakuOid)
                             (ok (access-controlled-application/get-application-list-by-haku hakuOid session organization-service))

                             (some? ssn)
                             (ok (access-controlled-application/get-application-list-by-ssn ssn session organization-service))

                             (some? dob)
                             (let [dob (dob/str->dob dob)]
                               (ok (access-controlled-application/get-application-list-by-dob dob session organization-service)))

                             (some? email)
                             (ok (access-controlled-application/get-application-list-by-email email session organization-service))
                             (some? name)
                             (ok (access-controlled-application/get-application-list-by-name name session organization-service))))

                  (api/GET "/:application-key" {session :session}
                    :path-params [application-key :- String]
                    :summary "Return application details needed for application review, including events and review data"
                    :return {:application       ataru-schema/Application
                             :events            [ataru-schema/Event]
                             :review            ataru-schema/Review
                             :hakukohde-reviews ataru-schema/HakukohdeReviews
                             :form              ataru-schema/FormWithContent}
                    (ok (application-service/get-application-with-human-readable-koodis application-key session organization-service tarjonta-service)))

                   (api/GET "/:application-key/modify" {session :session}
                     :path-params [application-key :- String]
                     :summary "Get HTTP redirect response for modifying a single application in Hakija side"
                     (if-let [virkailija-credentials (virkailija-edit/create-virkailija-credentials session application-key)]
                       (let [modify-url (str (-> config :public-config :applicant :service_url)
                                             "/hakemus?virkailija-secret="
                                             (:secret virkailija-credentials))]
                         (response/temporary-redirect modify-url))
                       (response/bad-request)))

                   (api/PUT "/review" {session :session}
                     :summary "Update existing application review"
                     :body [review ataru-schema/Review]
                     :return {:review            ataru-schema/Review
                              :events            [ataru-schema/Event]
                              :hakukohde-reviews ataru-schema/HakukohdeReviews}
                     (ok
                       (application-service/save-application-review
                         review
                         session
                         organization-service)))

                   (api/POST "/information-request/:application-key" {session :session}
                     :path-params [application-key :- s/Str]
                     :body [information-request ataru-schema/InformationRequest]
                     :summary "Send an information request to an applicant"
                     :return ataru-schema/InformationRequest
                     (ok (information-request/store information-request
                                                    application-key
                                                    session)))

                   (api/context "/excel" []
                     (api/GET "/form/:form-key" {session :session}
                              :path-params [form-key :- s/Str]
                              :query-params [{state :- [s/Str] nil}]
                              :summary "Return Excel export of the form and applications for it."
                              {:status  200
                               :headers {"Content-Type"        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                         "Content-Disposition" (str "attachment; filename=" (excel/filename-by-form form-key))}
                               :body    (application-service/get-excel-report-of-applications-by-form
                                          form-key
                                          state
                                          session
                                          organization-service
                                          tarjonta-service)})

                     (api/GET "/hakukohde/:hakukohde-oid" {session :session}
                              :path-params [hakukohde-oid :- s/Str]
                              :query-params [{state :- [s/Str] nil}]
                              :summary "Return Excel export of the hakukohde and applications for it."
                              {:status  200
                               :headers {"Content-Type"        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                         "Content-Disposition" (str "attachment; filename=" (excel/filename-by-hakukohde hakukohde-oid session organization-service tarjonta-service))}
                               :body    (application-service/get-excel-report-of-applications-by-hakukohde
                                          hakukohde-oid
                                          state
                                          session
                                          organization-service
                                          tarjonta-service)})

                     (api/GET "/haku/:haku-oid" {session :session}
                              :path-params [haku-oid :- s/Str]
                              :query-params [{state :- [s/Str] nil}]
                              :summary "Return Excel export of the haku and applications for it."
                              {:status  200
                               :headers {"Content-Type"        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                         "Content-Disposition" (str "attachment; filename=" (excel/filename-by-haku haku-oid session organization-service tarjonta-service))}
                               :body    (application-service/get-excel-report-of-applications-by-haku
                                          haku-oid
                                          state
                                          session
                                          organization-service
                                          tarjonta-service)})))

                 (api/context "/cache" []
                   (api/POST "/clear/:cache" {session :session}
                     :path-params [cache :- s/Str]
                     :summary "Clear an entire cache map of its entries"
                     {:status 200
                      :body   (do (cache/cache-clear cache-service (keyword cache))
                                  {})})
                   (api/POST "/remove/:cache/:key" {session :session}
                     :path-params [cache :- s/Str
                                   key :- s/Str]
                     :summary "Remove an entry from cache map"
                     {:status 200
                      :body   (do (cache/cache-remove cache-service (keyword cache) key)
                                  {})}))

                 (api/GET "/haut" {session :session}
                          :summary "List haku and hakukohde information found for applications stored in system"
                          :return ataru-schema/Haut
                          (ok {:tarjonta-haut (haku-service/get-haut session organization-service tarjonta-service)
                               :direct-form-haut (haku-service/get-direct-form-haut session organization-service)}))

                 (api/context "/koodisto" []
                              :tags ["koodisto-api"]
                              (api/GET "/" []
                                       :return s/Any
                                       (let [koodisto-list (koodisto/list-all-koodistos)]
                                         (ok koodisto-list)))
                              (api/GET "/:koodisto-uri/:version" [koodisto-uri version]
                                       :path-params [koodisto-uri :- s/Str version :- Long]
                                       :return s/Any
                                       (let [koodi-options (koodisto/get-koodisto-options koodisto-uri version)]
                                         (ok koodi-options))))

                 (api/context "/tarjonta" []
                              :tags ["tarjonta-api"]
                              (api/GET "/haku/:oid" []
                                       :path-params [oid :- (api/describe s/Str "Haku OID")]
                                       :return ataru-schema/Haku
                                       (if-let [haku (tarjonta/get-haku
                                                      tarjonta-service
                                                      oid)]
                                         (-> (tarjonta-service/parse-haku haku)
                                             ok
                                             (header "Cache-Control" "public, max-age=300"))
                                         (internal-server-error {:error "Internal server error"})))
                              (api/GET "/hakukohde" []
                                       :query-params [organizationOid :- (api/describe s/Str "Organization OID")
                                                      hakuOid :- (api/describe s/Str "Haku OID")]
                                       :return [ataru-schema/Hakukohde]
                                       (if-let [hakukohteet (tarjonta/hakukohde-search
                                                             tarjonta-service
                                                             hakuOid
                                                             organizationOid)]
                                         (-> hakukohteet
                                             ok
                                             (header "Cache-Control" "public, max-age=300"))
                                         (internal-server-error {:error "Internal server error"}))))

                 (api/context "/files" []
                   :tags ["files-api"]
                   (api/GET "/metadata" []
                     :query-params [key :- (api/describe [s/Str] "File key")]
                     :summary "Get metadata for one or more files"
                     :return [ataru-schema/File]
                     (if-let [resp (file-store/get-metadata key)]
                       (ok resp)
                       (not-found)))
                   (api/GET "/content/:key" []
                     :path-params [key :- (api/describe s/Str "File key")]
                     :summary "Download a file"
                     (if-let [file-response (file-store/get-file key)]
                       (header (ok (:body file-response))
                               "Content-Disposition"
                               (:content-disposition file-response))
                       (not-found))))

                 (api/context "/statistics" []
                   :tags ["statistics-api"]
                   (api/GET "/applications/:time-period" []
                            :path-params [time-period :- (api/describe (s/enum "month" "week" "day") "One of: month, week, day")]
                            :summary "Get info about number of submitted applications for past time period"
                            (ok (statistics-service/get-application-stats cache-service (keyword time-period)))))

                 (api/POST "/checkpermission" []
                           :body [dto ataru-schema/PermissionCheckDto]
                           :return ataru-schema/PermissionCheckResponseDto
                           (ok (permission-check/check organization-service dto)))

                 (api/context "/external" []
                   :tags ["external-api"]
                   (api/GET "/applications" {session :session}
                            :summary "Get the latest versions of applications in haku or hakukohde or by oids."
                            :query-params [{hakuOid :- s/Str nil}
                                           {hakukohdeOid :- s/Str nil}
                                           {hakemusOids :- [s/Str] nil}]
                            :return [ataru-schema/VtsApplication]
                            (if (and (nil? hakuOid)
                                     (nil? hakemusOids))
                              (response/bad-request {:error "No haku or application oid provided."})
                              (if-let [applications (access-controlled-application/vts-applications
                                                     organization-service
                                                     session
                                                     hakuOid
                                                     hakukohdeOid
                                                     hakemusOids)]
                                (response/ok applications)
                                (response/unauthorized {:error "Unauthorized"}))))
                   (api/GET "/persons" {session :session}
                            :summary "Get application-oid <-> person-oid mapping for haku or hakukohdes"
                            :query-params [hakuOid :- s/Str
                                           {hakukohdeOids :- [s/Str] nil}]
                            :return {s/Str s/Str}
                            (if-let [mapping (access-controlled-application/application-key-to-person-oid
                                              organization-service
                                              session
                                              hakuOid
                                              hakukohdeOids)]
                              (response/ok mapping)
                              (response/unauthorized {:error "Unauthorized"}))))))

(api/defroutes resource-routes
  (api/undocumented
    (route/resources "/lomake-editori")))

(api/defroutes rich-routes
  (api/undocumented
    (api/GET "/favicon.ico" []
      (-> "public/images/rich.jpg" io/resource))))

(defn- proxy-request [service-path request]
  (let [prefix   (str "https://" (get-in config [:urls :virkailija-host]) service-path)
        path     (-> request :params :*)
        response @(http/get (str prefix path) {:headers (:headers request)})]
    (assoc
     response
     ;; http-kit returns header names as keywords, but Ring requires strings :(
     :headers (map-kv
               (fn [header-kw header-value] [(name header-kw) header-value])
               (:headers request)))))

;; All these paths are required to be proxied by raamit when running locally
;; in your dev-environment. They will get proxied to the correct test environment
;; (e.g. untuva or qa)
(api/defroutes local-raami-routes
  (api/undocumented
   (api/GET "/virkailija-raamit/*" request
            :query-params [{fingerprint :- [s/Str] nil}]
            (proxy-request "/virkailija-raamit/" request))
   (api/GET "/authentication-service/*" request
            (proxy-request "/authentication-service/" request))
   (api/GET "/cas/*" request
            (proxy-request "/cas/" request))
   (api/GET "/lokalisointi/*" request
            (proxy-request "/lokalisointi/" request))))

(defn redirect-to-service-url
  []
  (redirect (get-in config [:public-config :virkailija :service_url])))

(api/defroutes redirect-routes
  (api/undocumented
    (api/GET "/" [] (redirect-to-service-url))
    ;; NOTE: This is now needed because of the way web-server is
    ;; Set up on test and other environments. If you want
    ;; to remove this, test the setup with some local web server
    ;; with proxy_pass /lomake-editori -> <clj server>/lomake-editori
    ;; and verify that it works on test environment as well.
    (api/GET "/lomake-editori" [] (redirect-to-service-url))))

(api/defroutes dashboard-routes
  (api/undocumented
    (api/GET "/dashboard" []
      (selmer/render-file "templates/dashboard.html" {}))))

(defrecord Handler []
  component/Lifecycle

  (start [this]
    (assoc this :routes (-> (api/api
                              {:swagger {:spec "/lomake-editori/swagger.json"
                                         :ui "/lomake-editori/api-docs"
                                         :data {:info {:version "1.0.0"
                                                       :title "Ataru Clerk API"
                                                       :description "Specifies the clerk API for Ataru"}
                                                :tags [{:name "form-api" :description "Form handling"}
                                                       {:name "applications-api" :description "Application handling"}
                                                       {:name "koodisto-api" :description "Koodisto service"}
                                                       {:name "files-api" :description "File service"}]}}
                               :exceptions {:handlers {::ex/request-parsing
                                                       (ex/with-logging ex/request-parsing-handler :warn)
                                                       ::ex/request-validation
                                                       (ex/with-logging ex/request-validation-handler :warn)
                                                       ::ex/response-validation
                                                       (ex/with-logging ex/response-validation-handler :error)
                                                       ::ex/default
                                                       (ex/with-logging ex/safe-handler :error)}}}
                              redirect-routes
                              (when (:dev? env) rich-routes)
                              (when (:dev? env) local-raami-routes)
                              resource-routes
                              (api/context "/lomake-editori" []
                                test-routes
                                dashboard-routes
                                (api/middleware [user-feedback/wrap-user-feedback
                                                 wrap-database-backed-session
                                                 auth-middleware/with-authentication]
                                  (api/middleware [session-timeout/wrap-idle-session-timeout]
                                    app-routes
                                    (api-routes this))
                                  (auth-routes (:organization-service this))))
                              (api/undocumented
                                (route/not-found "Not found")))
                            (wrap-defaults (-> site-defaults
                                               (assoc :session nil)
                                               (update :responses dissoc :content-types)
                                               (update :security dissoc :content-type-options :anti-forgery)))
                            (wrap-with-logger
                              :debug identity
                              :info  (fn [x] (info x))
                              :warn  (fn [x] (warn x))
                              :error (fn [x] (error x))
                              :pre-logger (fn [_ _] nil)
                              :post-logger (fn [options {:keys [uri] :as request} {:keys [status] :as response} totaltime]
                                             (when (or
                                                     (>= status 400)
                                                     (clojure.string/starts-with? uri "/lomake-editori/api/"))
                                               (#'middleware-logger/post-logger options request response totaltime))))
                            (wrap-gzip)
                            (cache-control/wrap-cache-control))))

  (stop [this]
    (when
      (contains? this :routes)
      (assoc this :routes nil))))

(defn new-handler []
  (->Handler))
