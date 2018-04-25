(ns ataru.virkailija.editor.components.followup-question
  (:require
   [ataru.cljs-util :as util]
   [ataru.component-data.component :as component]
   [ataru.virkailija.editor.components.toolbar :as toolbar]
   [cljs.core.match :refer-macros [match]]
   [re-frame.core :refer [subscribe dispatch reg-sub reg-event-db reg-fx reg-event-fx]]
   [reagent.core :as r]
   [reagent.ratom :refer-macros [reaction]]
   [taoensso.timbre :refer-macros [spy debug]]
   [ataru.virkailija.temporal :as temporal]))

(reg-sub :editor/followup-overlay
  (fn [db [_ option-path]]
    (get-in db [:editor :followup-overlay option-path :visible?])))

(reg-event-db
  :editor/followup-overlay-open
  (fn [db [_ option-path]]
    (assoc-in db [:editor :followup-overlay option-path :visible?] true)))

(reg-event-db
  :editor/followup-overlay-close
  (fn [db [_ option-path]]
    (assoc-in db [:editor :followup-overlay option-path :visible?] false)))

(reg-event-db
  :editor/generate-followup-component
  (fn [db [_ generate-fn option-path]]
    (let [user-info (-> db :editor :user-info)
          metadata  {:oid  (:oid user-info)
                     :name (:name user-info)
                     :date (temporal/datetime-now)}
          component (generate-fn {:created-by  metadata
                                  :modified-by metadata})]
      (update-in db (util/flatten-path db option-path :followups) (fnil conj []) component))))

(defn followup-question-overlay [option-index option-path show-followups]
  (let [layer-visible? (subscribe [:editor/followup-overlay option-path :visible?])
        followups      (subscribe [:editor/get-component-value (flatten [option-path :followups])])]
    (fn [option-index option-path show-followups]
      (when (or @layer-visible? (and (get @show-followups option-index) (not-empty @followups)))
        [:div.editor-form__followup-question-overlay-parent
         [:div.editor-form__followup-question-overlay-outer
          [:div.editor-form__followup-indicator]
          [:div.editor-form__followup-indicator-inlay]
          [:div.editor-form__followup-question-overlay
           (into [:div]
             (for [[index followup] (map vector (range) @followups)]
               [ataru.virkailija.editor.core/soresu->reagent followup (vec (flatten [option-path :followups index]))]))
           [toolbar/followup-toolbar option-path
            (fn [generate-fn]
              (dispatch [:editor/generate-followup-component generate-fn option-path]))]]]]))))

(defn followup-question [option-index option-path show-followups]
  (let [layer-visible?        (subscribe [:editor/followup-overlay option-path :visible?])
        followup-component    (subscribe [:editor/get-component-value (vec (flatten [option-path :followups]))])
        allow-more-followups? (->> option-path
                                   flatten
                                   (filter #(= :followups %))
                                   count
                                   (> 2))]
    (fn [option-index option-path show-followups]
      [:div.editor-form__followup-question
       (when allow-more-followups?
         (match [@followup-component @layer-visible?]
           [(_ :guard not-empty) _] [:a
                                     {:on-click #(swap! show-followups
                                                   (fn [v] (assoc v option-index
                                                                    (not (get v option-index)))))}
                                     (str "Lisäkysymykset (" (count @followup-component) ") ")
                                     (if (get @show-followups option-index)
                                       [:i.zmdi.zmdi-chevron-up.zmdi-hc-lg]
                                       [:i.zmdi.zmdi-chevron-down.zmdi-hc-lg])]
           [_ true] [:a {:on-click #(dispatch [:editor/followup-overlay-close option-path])} "Lisäkysymykset"]
           :else [:a {:on-click #(dispatch [:editor/followup-overlay-open option-path])} "Lisäkysymykset"]))])))
