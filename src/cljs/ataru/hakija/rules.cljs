(ns ataru.hakija.rules
  (:require [cljs.core.match :refer-macros [match]]))

(defn swap-ssn-birthdate-based-on-nationality
  [db _]
  (let [nationality (-> db :application :answers :nationality)]
    (if-let [value (and (:valid nationality) (not-empty (:value nationality)))]
      (match value
        "Suomi"
        (-> db
            (update-in [:application :answers :birth-date] dissoc :value)
            (update-in [:application :ui :birth-date] assoc :visible? false)
            (update-in [:application :ui :ssn] assoc :visible? true))

        (_ :guard string?)
        (-> db
            (update-in [:application :answers :ssn] dissoc :value)
            (update-in [:application :ui :ssn] assoc :visible? false)
            (update-in [:application :ui :birth-date] assoc :visible? true)))
      (-> db
          (update-in [:application :answers :birth-date] dissoc :value)
          (update-in [:application :answers :ssn] dissoc :value)
          (update-in [:application :ui :birth-date] assoc :visible? false)
          (update-in [:application :ui :ssn] assoc :visible? false)))))

(defn- hakija-rule-to-fn [rule]
  (case rule
         :swap-ssn-birthdate-based-on-nationality
         swap-ssn-birthdate-based-on-nationality
         nil))

(defn extract-rules [content]
  (->> (for [field content]
         (if-let [children (:children field)]
           (extract-rules children)
           (:rules field)))
       flatten
       (filter not-empty)
       vec))

(defn run-rule
  ([rule db]
   (run-rule hakija-rule-to-fn rule db))
  ([rule-to-fn rule db]
   {:pre [(map? rule)
          (map? db)]}
   (reduce-kv
     (fn [db-accumulator rule argument]
       (if-let [rule-fn (rule-to-fn rule)]
         (or (rule-fn db-accumulator argument)
             db-accumulator)
         db-accumulator))
     db
     rule)))

(defn run-all-rules [db]
  (reduce
    (fn [db-accumulator rule]
      (run-rule rule db-accumulator))
    db
    (extract-rules (-> db :form :content))))
