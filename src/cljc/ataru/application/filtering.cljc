(ns ataru.application.filtering
  (:require [ataru.application.review-states :as review-states]
            [clojure.set :as set]))

(defn filter-by-hakukohde-review
  [application selected-hakukohteet requirement-name states-to-include]
  (let [all-states-count (-> review-states/hakukohde-review-types-map
                             (get (keyword requirement-name))
                             (last)
                             (count))
        selected-count   (count states-to-include)]
    (if (= all-states-count selected-count)
      true
      (let [relevant-states  (->> (:application-hakukohde-reviews application)
                                  (filter #(and (= requirement-name (:requirement %))
                                                (or (not selected-hakukohteet) (contains? selected-hakukohteet (:hakukohde %)))))
                                  (map :state)
                                  (set))]
        (not (empty? (set/intersection states-to-include relevant-states)))))))
