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
  ([path dispatch-path?]
   (.pushState js/history nil nil path)
   (when dispatch-path?
     (secretary/dispatch! path)))
  ([path]
    (set-history! path true)))

(defn anchor-click-handler
  [event]
  (let [path (.getPath (.parse Uri (.-href (.-target event))))
        matches-path? (secretary/locate-route path)]
    (when matches-path?
      (set-history! path false))
    (.preventDefault event)))

(defn app-routes []
  (defroute "/lomake-editori/" []
    (secretary/dispatch! "/lomake-editori/editor"))

  (defroute "/lomake-editori/editor" []
    (dispatch [:set-active-panel :editor])
    (dispatch [:editor/select-form nil])
    (dispatch [:editor/refresh-forms]))

  (defroute #"^/lomake-editori/editor/(.*)" [key]
    (dispatch [:set-active-panel :editor])
    (dispatch [:editor/refresh-forms])
    (dispatch-after-state
     :predicate
     (fn [db]
       (not-empty (get-in db [:editor :forms key])))
     :handler
     (fn [form]
       (dispatch [:editor/select-form (:key form)]))))

  (defroute #"^/lomake-editori/applications/" []
    (dispatch [:editor/refresh-forms])
    (dispatch-after-state
     :predicate
     (fn [db] (not-empty (get-in db [:editor :forms])))
     :handler
     (fn [forms]
       (let [form (-> forms first second)]
         (.replaceState js/history nil nil (str "/lomake-editori/applications/" (:key form)))
         (dispatch [:editor/select-form (:key form)])
         (dispatch [:application/fetch-applications (:key form)])))
     (dispatch [:set-active-panel :application])))

  (defroute #"^/lomake-editori/applications/(.*)" [key]
    (dispatch [:editor/refresh-forms])
    (dispatch-after-state
     :predicate
     (fn [db] (not-empty (get-in db [:editor :forms key])))
     :handler
     (fn [form]
       (dispatch [:editor/select-form (:key form)])
       (dispatch [:application/fetch-applications (:key form)])))
    (dispatch [:set-active-panel :application]))

  (accountant/dispatch-current!))
