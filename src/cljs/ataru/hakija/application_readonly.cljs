(ns ataru.hakija.application-readonly
  (:require [clojure.string :refer [trim]]
            [re-frame.core :refer [subscribe]]
            [cljs.core.match :refer-macros [match]]
            [ataru.hakija.application-field-common :refer [answer-key
                                                           required-hint
                                                           textual-field-value
                                                           wrapper-id]]))

(defn text [field-descriptor]
  (let [application (subscribe [:state-query [:application]])
        label (-> field-descriptor :label :fi)]
    (fn [field-descriptor]
      [:div.application__form-field
       [:label.application__form-field-label (str label (required-hint field-descriptor))]
       [:div (textual-field-value field-descriptor @application)]])))

(declare field)

(defn wrapper [content children]
  (let [fieldset? (= "fieldset" (:fieldType content))]
    [:div.application__wrapper-element
     (when fieldset?
       {:class "application__wrapper-element--border"})
     [:h2.application__wrapper-heading
      {:id (wrapper-id content)}
      (-> content :label :fi)]
     (into [:div (when fieldset? {:class "application__wrapper-contents"})] (mapv field children))]))

(defn field
  [content]
  (match content
         {:fieldClass "wrapperElement" :children children} [wrapper content children]
         {:fieldClass "formField" :fieldType (:or "textField" "textArea" "dropdown")} [text content]))

(defn render-readonly-fields [form-data]
  (when form-data
    (mapv field (:content form-data))))
