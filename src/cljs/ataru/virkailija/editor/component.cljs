(ns ataru.virkailija.editor.component
  (:require [ataru.virkailija.soresu.component :as component]
            [reagent.core :as r]
            [cljs.core.match :refer-macros [match]]
            [re-frame.core :refer [subscribe dispatch]]))

(defn language [lang]
  (fn [lang]
    [:div.language
     [:div (clojure.string/upper-case (name lang))]]))

(def ^:private checkbox-metadata
  {:required {:id-suffix "_required"
              :label "Pakollinen tieto"}})

(defn- render-checkbox
  [path initial-content metadata-kwd]
  (let [metadata (get checkbox-metadata metadata-kwd)
        id (str (gensym) (:id-suffix metadata))
        label (:label metadata)]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type "checkbox"
                                    :id id
                                    :checked (true? (get initial-content metadata-kwd))
                                    :on-change #(dispatch [:editor/set-component-value (-> % .-target .-checked) path metadata-kwd])}]
     [:label.editor-form__checkbox-label {:for id} label]]))

(defn- text-header
  [label path]
  [:div.editor-form__header-wrapper
   [:header.editor-form__component-header label]
   [:a.editor-form__component-header-link
    {:on-click #(dispatch [:hide-component path])}
    "Poista"]])

(defn text-component [initial-content path & {:keys [header-label size-label]}]
  (let [languages        (subscribe [:editor/languages])
        value            (subscribe [:editor/get-component-value path])
        size             (subscribe [:editor/get-component-value path :params :size])
        radio-group-id   (str "form-size-" (gensym))
        radio-buttons    ["S" "M" "L"]
        radio-button-ids (reduce (fn [acc btn] (assoc acc btn (str radio-group-id "-" btn))) {} radio-buttons)
        size-change      (fn [new-size] (dispatch [:editor/set-component-value new-size path :params :size]))
        handler-fn       (fn [_]
                           (dispatch [:remove-component path]))]
    (r/create-class
      {:component-did-mount
       (fn [this]
         (let [node   (r/dom-node this)
               events ["webkitAnimationEnd" "mozAnimationEnd" "MSAnimationEnd" "oanimationend" "animationend"]]
           (doseq [event events]
             (.addEventListener node event handler-fn))))
       :reagent-render
       (fn [initial-content path & {:keys [header-label size-label]}]
         [:div.editor-form__component-wrapper
          (when
            (= "fading-out" (get-in initial-content [:params :status]))
            {:class "animated fadeOutUp"})
          [text-header header-label path]
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header "Otsikko"]
           (doall
             (for [lang @languages]
               ^{:key lang}
               [:input.editor-form__text-field {:value     (get-in @value [:label lang])
                                                :on-change #(dispatch [:editor/set-component-value (-> % .-target .-value) path :label lang])}]))]
          [:div.editor-form__size-button-wrapper
           [:header.editor-form__component-item-header size-label]
           [:div.editor-form__size-button-group
            (doall (for [[btn-name btn-id] radio-button-ids]
                     ^{:key (str btn-id "-radio")}
                     [:div
                      [:input.editor-form__size-button.editor-form__size-button
                       {:type      "radio"
                        :value     btn-name
                        :checked   (or
                                     (= @size btn-name)
                                     (and
                                       (nil? @size)
                                       (= "M" btn-name)))
                        :name      radio-group-id
                        :id        btn-id
                        :on-change (fn [] (size-change btn-name))}]
                      [:label
                       {:for btn-id
                        :class     (match btn-name
                                     "S" "editor-form__size-button--left-edge"
                                     "L" "editor-form__size-button--right-edge"
                                     :else nil)}
                       btn-name]]))]]
          [:div.editor-form__checkbox-wrapper
           (render-checkbox path initial-content :required)]])})))

(defn text-field [initial-content path]
  [text-component initial-content path :header-label "Tekstikenttä" :size-label "Tekstikentän leveys"])

(defn text-area [initial-content path]
  [text-component initial-content path :header-label "Tekstialue" :size-label "Tekstialueen leveys"])

(def ^:private toolbar-elements
  {"Lomakeosio"                component/form-section
   "Tekstikenttä"              component/text-field
   "Tekstialue"                component/text-area})

(defn ^:private component-toolbar [path]
  (fn [path]
    (into [:ul.form__add-component-toolbar--list]
          (for [[component-name generate-fn] toolbar-elements
                :when                        (not (and
                                                    (vector? path)
                                                    (= :children (second path))
                                                    (= "Lomakeosio" component-name)))]
            [:li.form__add-component-toolbar--list-item {:on-click #(dispatch [:generate-component generate-fn path])}
             component-name]))))

(defn add-component [path]
  (let [show-bar? (r/atom nil)
        show-bar #(reset! show-bar? true)
        hide-bar #(reset! show-bar? false)]
    (fn [path]
      (if @show-bar?
        [:div.editor-form__add-component-toolbar
         {:on-mouse-leave hide-bar
          :on-mouse-enter show-bar}
         [component-toolbar path]]
        [:div.editor-form__add-component-toolbar
         {:on-mouse-enter show-bar
          :on-mouse-leave hide-bar}
         [:div.plus-component
          [:span "+"]]]))))

(defn form-component-group [path]
  (let [languages (subscribe [:editor/languages])
        value     (subscribe [:editor/get-component-value path])]
    (fn [path]
      (-> [:div.editor-form__component-wrapper
           [text-header "Lomakeosio" path]]
          (into
            [[:div.editor-form__text-field-wrapper.editor-form__text-field--section
              [:header.editor-form__component-item-header "Osion nimi"]
              (doall
                (for [lang @languages]
                  ^{:key lang}
                  [:input.editor-form__text-field
                   {:value     (get-in @value [:label lang])
                    :on-change #(dispatch [:editor/set-component-value (-> % .-target .-value) path :label lang])}]))]])))))
