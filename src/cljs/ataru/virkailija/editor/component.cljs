(ns ataru.virkailija.editor.component
  (:require [ataru.virkailija.soresu.component :as component]
            [reagent.core :as r]
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

(defn- render-component-header
  [label path]
  [:div.editor-form__header-wrapper
   [:header.editor-form__component-header label]
   [:a.editor-form__component-header-link
    {:on-click #(dispatch [:remove-component path])}
    "Poista"]])

(defn render-text-field [initial-content path]
  (let [languages        (subscribe [:editor/languages])
        value            (subscribe [:editor/get-component-value path])
        size             (subscribe [:editor/get-component-value path :params :size])
        radio-group-id   (str "form-size-" (gensym))
        radio-buttons    ["S" "M" "L"]
        radio-button-ids (reduce (fn [acc btn] (assoc acc btn (str radio-group-id "-" btn))) {} radio-buttons)
        size-change      (fn [new-size] (dispatch [:editor/set-component-value new-size path :params :size]))]
    (fn [initial-content path]
      [:div.editor-form__component-wrapper
       (render-component-header "Tekstikenttä" path)
       [:div.editor-form__text-field-wrapper
        [:header.editor-form__component-item-header "Otsikko"]
        (doall
          (for [lang @languages]
            ^{:key lang}
            [:input.editor-form__text-field {
                                             :value     (get-in @value [:label lang])
                                             :on-change #(dispatch [:editor/set-component-value (-> % .-target .-value) path :label lang])}]))]
       [:div.editor-form__size-button-wrapper
        [:header.editor-form__component-item-header "Tekstikentän leveys"]
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
                     {:for btn-id} btn-name]]))]]
       [:div.editor-form__checkbox-wrapper
        (render-checkbox path initial-content :required)]])))

(defn render-link-info [{:keys [params] :as content} path]
  (let [languages (subscribe [:editor/languages])
        value     (subscribe [:editor/get-component-value path])]
    (fn [{:keys [params] :as content} path]
      (into
        [:div.link-info
         [:p "Linkki"]]
        (for [lang @languages]
          [:div
           [:p "Osoite"]
           [:input {:value       (get-in @value [:params :href lang])
                    :type        "url"
                    :on-change   #(dispatch [:editor/set-component-value (-> % .-target .-value) path :params :href lang])
                    :placeholder "http://"}]
           [language lang]
           [:input {:on-change   #(dispatch [:editor/set-component-value (-> % .-target .-value) path :text lang])
                    :value       (get-in @value [:text lang])
                    :placeholder "Otsikko"}]
           [language lang]])))))

(defn render-info [{:keys [params] :as content} path]
  (let [languages (subscribe [:editor/languages])
        value     (subscribe [:editor/get-component-value path :text])]
    (fn [{:keys [params] :as content} path]
      (into
        [:div.info
         [:p "Ohjeteksti"]]
        (for [lang @languages]
          [:div
           [:input
            {:value       (get @value lang)
             :on-change   #(dispatch [:editor/set-component-value (-> % .-target .-value) path :text lang])
             :placeholder "Ohjetekstin sisältö"}]
           [language lang]
           ])))))

(def ^:private toolbar-elements
  (let [dummy [:div "ei vielä toteutettu.."]]
    {"Lomakeosio"                component/form-section
     "Tekstikenttä"              component/text-field}))
;"Tekstialue"                dummy
;"Lista, monta valittavissa" dummy
;"Lista, yksi valittavissa"  dummy
;"Pudotusvalikko"            dummy
;"Vierekkäiset kentät"       dummy
;"Liitetiedosto"             dummy
;"Ohjeteksti"                info
;"Linkki"                    link-info
;"Väliotsikko"               dummy

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

(defn section-label [path]
  (let [languages (subscribe [:editor/languages])
        value     (subscribe [:editor/get-component-value path])]
    (fn []
      (-> [:div.editor-form__component-wrapper
           (render-component-header "Lomakeosio" path)]
          (into
            [[:div.editor-form__text-field-wrapper
              [:header.editor-form__component-item-header "Osion nimi"]
              (doall
                (for [lang @languages]
                  ^{:key lang}
                  [:input.editor-form__text-field {:value     (get-in @value [:label lang])
                                                   :on-change #(dispatch [:editor/set-component-value (-> % .-target .-value) path :label lang])}]))]])))))
