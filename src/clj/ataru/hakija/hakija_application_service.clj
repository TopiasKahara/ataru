(ns ataru.hakija.hakija-application-service
  (:require
   [taoensso.timbre :as log]
   [ataru.background-job.job :as job]
   [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
   [ataru.hakija.application-email-confirmation :as application-email]
   [ataru.person-service.person-integration :as person-integration]
   [ataru.forms.form-store :as form-store]
   [ataru.hakija.validator :as validator]
   [ataru.applications.application-store :as application-store]))

(defn- store-and-log [application]
  (let [application-id (application-store/add-application-or-increment-version! application)]
        (log/info "Stored application with id: " application-id)
        {:passed?        true
         :application-id application-id}))

;; (defn add-application-old [application]
;;   (let [form              (form-store/fetch-by-id (:form application))
;;         validation-result (validator/valid-application? application form)]
;;     (cond
;;       (:deleted form)
;;       (handle-deleted (:id form))

;;       (:passed? validation-result)
;;       (store-and-log application)

;;       :else validation-result)))

(defn add-application [application]
  (let [form              (form-store/fetch-by-id (:form application))
        validation-result (validator/valid-application? application form)]
    (if (:passed? validation-result)
      (store-and-log application)
      validation-result)))

(defn start-submit-jobs [application-id]
  (let [person-service-job-id (job/start-job hakija-jobs/job-definitions
                                             (:type person-integration/job-definition)
                                             {:application-id application-id})]
    (application-email/start-email-confirmation-job application-id)
    (log/info "Stored application with id:" application-id)
    (log/info "Started person creation job (to person service) with job id" person-service-job-id)
    {:passed? true :id application-id}))

(defn handle-application-submit [application]
  (log/info "Application submitted:" application)
  (let [{passed? :passed?
         failures :failures
         application-id :application-id
         :as validation-result}
        (add-application application)]
    (if passed?
      (start-submit-jobs application-id)
      validation-result)))
