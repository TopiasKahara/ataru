(ns ataru.virkailija.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import [goog Uri])
  (:require [ataru.cljs-util :refer [dispatch-after-state]]
            [secretary.core :as secretary]
            [re-frame.core :refer [dispatch]]
            [accountant.core :as accountant]))

(accountant/configure-navigation! {:nav-handler  (fn [path]
                                                   (secretary/dispatch! path))
                                   :path-exists? (fn [path]
                                                   (secretary/locate-route path))})

(defn set-history!
  [path]
  (accountant/navigate! path))

(defn navigate-to-click-handler
  [path & _]
  (when (secretary/locate-route path)
    (set-history! path)))

(defn- select-editor-form-if-not-deleted
  [form]
  (if (:deleted form)
    (do
      (.replaceState js/history nil nil "/lomake-editori/editor")
      (secretary/dispatch! "/lomake-editori/editor"))
    (dispatch [:editor/select-form (:key form)])))

(defn common-actions-for-applications-route []
  (dispatch [:application/refresh-haut2]) ;; TODO this happens too often!
  (dispatch [:application/close-search-control])
  (dispatch [:set-active-panel :application]))

(defn app-routes []
  (defroute "/lomake-editori/" []
    (secretary/dispatch! "/lomake-editori/editor"))

  (defroute "/lomake-editori/editor" []
    (dispatch [:set-active-panel :editor])
    (dispatch [:editor/select-form nil])
    (dispatch [:editor/refresh-forms-for-editor])
    (dispatch [:editor/refresh-forms-in-use]))

  (defroute #"^/lomake-editori/editor/(.*)" [key]
    (dispatch [:set-active-panel :editor])
    (dispatch [:editor/refresh-forms-if-empty key])
    (dispatch [:editor/refresh-forms-in-use])
    (dispatch-after-state
     :predicate
     (fn [db]
       (not-empty (get-in db [:editor :forms key])))
     :handler select-editor-form-if-not-deleted))

  (defroute #"^/lomake-editori/applications/" []
    (common-actions-for-applications-route))

  (defroute #"^/lomake-editori/applications/hakukohde/(.*)" [hakukohde-oid]
    (common-actions-for-applications-route)

    (dispatch-after-state
     :predicate
     (fn [db]
       (some #(when (= hakukohde-oid (:oid %)) %)
             (get-in db [:application :hakukohteet2])))
     :handler
     (fn [hakukohde]
       (dispatch [:application/select-hakukohde hakukohde])
       (dispatch [:application/fetch-applications-by-hakukohde hakukohde-oid]))))

  (defroute #"^/lomake-editori/applications/haku/(.*)" [haku-oid]
    (common-actions-for-applications-route)

    (dispatch-after-state
     :predicate
     (fn [db]
       (some #(when (= haku-oid (:oid %)) %)
             (get-in db [:application :haut2 :tarjonta-haut])))
     :handler
     (fn [haku]
       (dispatch [:application/select-haku haku])
       (dispatch [:application/fetch-applications-by-haku haku-oid]))))

  (defroute #"^/lomake-editori/applications/(.*)" [key]
    (common-actions-for-applications-route)
    (dispatch-after-state
     :predicate
     (fn [db] (not-empty (get-in db [:application :forms key])))
     :handler
     (fn [form]
       (dispatch [:application/select-form (:key form)])
       (dispatch [:application/fetch-applications (:key form)]))))

  (accountant/dispatch-current!))
