(ns ataru.component-data.value-transformers
  #?(:cljs (:require [goog.string :as gstring])))

(def ^:private dob-pattern #"^(\d{1,2})\.(\d{1,2})\.(\d{4})$")

(defn birth-date [dob]
  (when-let [[_ day month year] (re-matches dob-pattern dob)]
    (let [f #?(:clj format :cljs gstring/format)]
      (f "%02d.%02d.%d" #?@(:clj  [(Integer/valueOf day)
                                   (Integer/valueOf month)
                                   (Integer/valueOf year)]
                            :cljs [day month year])))))

(defn update-options-while-keeping-existing-followups [newest-options existing-options]
  (if (empty? existing-options)
    newest-options
    (let [existing-values                 (set (map :value existing-options))
          options-that-didnt-exist-before (filter #(not (contains? existing-values (:value %))) newest-options)]
      (concat options-that-didnt-exist-before
              (map (fn [existing-option]
                     (if-let [new-option (first (filter #(= (:value %) (:value existing-option)) newest-options))]
                       (if-let [existing-folluwups (:followups existing-option)]
                         (assoc new-option :followups existing-folluwups)
                         existing-option)
                       existing-option))
                   existing-options)))))
