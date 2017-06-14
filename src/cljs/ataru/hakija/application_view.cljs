(ns ataru.hakija.application-view
  (:require [clojure.string :refer [trim]]
            [ataru.hakija.banner :refer [banner]]
            [ataru.hakija.application-form-components :refer [editable-fields]]
            [ataru.hakija.hakija-readonly :as readonly-view]
            [ataru.cljs-util :as util]
            [ataru.translations.translation-util :refer [get-translations]]
            [ataru.translations.application-view :as translations]
            [ataru.hakija.application :refer [application-in-complete-state?]]
            [ataru.application-common.koulutus :as koulutus]
            [re-frame.core :refer [subscribe dispatch]]
            [cljs.core.match :refer-macros [match]]
            [cljs-time.format :refer [unparse formatter]]
            [cljs-time.coerce :refer [from-long]]
            [clojure.string :as string]
            [goog.string :as gstring]
            [reagent.ratom :refer [reaction]]))

(def ^:private language-names
  {:fi "Suomeksi"
   :sv "På svenska"
   :en "In English"})

(def date-format (formatter "d.M.yyyy"))

(defn application-header [form]
  (let [selected-lang     (:selected-language form)
        languages         (filter
                            (partial not= selected-lang)
                            (:languages form))
        submit-status     (subscribe [:state-query [:application :submit-status]])
        application       (subscribe [:state-query [:application]])
        secret            (:modify (util/extract-query-params))
        hakukohde-name    (-> form :tarjonta :hakukohde-name)
        haku-tarjoja-name (-> form :tarjonta :haku-tarjoaja-name)
        koulutukset-str   (koulutus/koulutukset->str (-> form :tarjonta :koulutukset))
        apply-start-date  (-> form :tarjonta :hakuaika-dates :start)
        apply-end-date    (-> form :tarjonta :hakuaika-dates :end)
        hakuaika-on       (-> form :tarjonta :hakuaika-dates :on)
        translations      (get-translations
                           (keyword selected-lang)
                           translations/application-view-translations)
        apply-dates       (when hakukohde-name
                            (if (and apply-start-date apply-end-date)
                              (str (:application-period translations)
                                   ": "
                                   (unparse date-format (from-long apply-start-date))
                                   " - "
                                   (unparse date-format (from-long apply-end-date))
                                   (when-not hakuaika-on
                                     (str " (" (:not-within-application-period translations) ")")))
                              (:continuous-period translations)))]
    (fn [form]
      [:div
       [:div.application__header-container
        [:span.application__header (or hakukohde-name (:name form))]
        (when (and (not= :submitted @submit-status)
                   (> (count languages) 0)
                   (nil? secret))
          [:span.application__header-text
           (map-indexed (fn [idx lang]
                          (cond-> [:span {:key (name lang)}
                                   [:a {:href (str "?lang=" (name lang))}
                                    (get language-names lang)]]
                                  (> (dec (count languages)) idx)
                                  (conj [:span.application__header-language-link-separator " | "])))
                        languages)])]
       (when (and haku-tarjoja-name apply-dates)
         [:div.application__sub-header-container
          (when-not (string/blank? koulutukset-str) [:div.application__sub-header-koulutus koulutukset-str])
          [:span.application__sub-header-organization haku-tarjoja-name]
          [:span.application__sub-header-dates apply-dates]])
       (when (application-in-complete-state? @application)
         [:div.application__sub-header-container
          [:span.application__sub-header-modifying-prevented
           (:application-processed-cant-modify translations)]])])))

(defn readonly-fields [form]
  (let [application (subscribe [:state-query [:application]])]
    (fn [form]
      [readonly-view/readonly-fields form @application])))

(defn render-fields [form]
  (let [submit-status (subscribe [:state-query [:application :submit-status]])]
    (fn [form]
      (if (= :submitted @submit-status)
        [readonly-fields form]
        (do
          (dispatch [:application/run-rule])
          [editable-fields form])))))

(defn application-contents []
  (let [form       (subscribe [:state-query [:form]])
        can-apply? (subscribe [:application/can-apply?])]
    (fn []
      [:div.application__form-content-area
       ^{:key (:id @form)}
       [application-header @form]
       (when @can-apply?
         [render-fields @form])])))

(defn- star-number-from-event
  [event]
  (js/parseInt (.-starN (.-dataset (.-target event))) 10))

(defn feedback-form
  []
  (let [submit-status  (subscribe [:state-query [:application :submit-status]])
        star-hovered   (subscribe [:state-query [:application :feedback :star-hovered]])
        stars          (subscribe [:state-query [:application :feedback :stars]])
        hidden?        (subscribe [:state-query [:application :feedback :hidden?]])
        rating-status  (subscribe [:state-query [:application :feedback :status]])
        show-feedback? (reaction (and (= :submitted @submit-status)
                                      (not @hidden?)))]
    (fn []
      (when @show-feedback?
        [:div.application-feedback-form
         [:a.application-feedback-form__close-button
          {:on-click #(dispatch [:application/rating-form-toggle])}
          [:i.zmdi.zmdi-close.close-details-button-mark]]
         [:div.application-feedback-form-container
          (when (not= :feedback-submitted @rating-status)
            [:h1.application-feedback-form__header "Hei, kerro vielä mitä pidit hakulomakkeesta!"])
          (when (not= :feedback-submitted @rating-status)
            [:div.application-feedback-form__rating-container
             {:on-click      #(dispatch [:application/rating-submit (star-number-from-event %)])
              :on-mouse-out  #(dispatch [:application/rating-hover 0])
              :on-mouse-over #(dispatch [:application/rating-hover (star-number-from-event %)])}
             (let [stars-active (or @stars @star-hovered 0)]
               (map (fn [n]
                      (let [star-classes (if (< n stars-active)
                                           :i.application-feedback-form__rating-star--active.zmdi.zmdi-star.zmdi-hc-5x
                                           :i.application-feedback-form__rating-star--inactive.zmdi.zmdi-star-outline.zmdi-hc-5x)]
                        [star-classes
                         {:key         (str "rating-star-" n)
                          :data-star-n (inc n)}])) (range 5)))])
          (when (not= :feedback-submitted @rating-status)
            [:div.application-feedback-form__rating-text
             (case (or @stars @star-hovered)
               1 "Huono :'("
               2 "Välttävä :("
               3 "Tyydyttävä :|"
               4 "Hyvä :)"
               5 "Kiitettävä :D"
               (gstring/unescapeEntities "&nbsp;"))])
          (when (= :rating-given @rating-status)
            [:div.application-feedback-form__text-feedback-container
             [:textarea.application__form-text-input.application__form-text-area.application__form-text-area__size-medium
              {:on-change   #(dispatch [:application/rating-update-feedback (.-value (.-target %))])
               :placeholder "Anna halutessasi kehitysideoita tai kommentteja hakijan palvelusta"
               :max-length  2000}]])
          (when (= :rating-given @rating-status)
            [:a.application__send-feedback-button
             {:on-click (fn [evt]
                          (.preventDefault evt)
                          (dispatch [:application/rating-feedback-submit]))} "Lähetä palaute"])
          (when (= :feedback-submitted @rating-status)
            [:div "Kiitos palautteestasi!"])]]))))

(defn error-display []
  (let [error-message (subscribe [:state-query [:error :message]])
        detail (subscribe [:state-query [:error :detail]])]
    (fn [] (if @error-message
             [:div.application__error-display @error-message (str @detail)]
             nil))))

(defn form-view []
  [:div
   [banner]
   [error-display]
   [application-contents]
   [feedback-form]])
