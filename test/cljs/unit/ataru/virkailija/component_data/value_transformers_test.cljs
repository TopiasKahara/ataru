(ns ataru.virkailija.component-data.value-transformers-test
  (:require [ataru.virkailija.component-data.value-transformers :as t])
  (:require-macros [cljs.test :refer [deftest is]]))

(deftest transforms-dob-string
  (doall
    (for [[expected dob] [["01.10.2010" "1.10.2010"]
                          ["10.01.2010" "10.1.2010"]
                          ["01.01.2010" "1.1.2010"]
                          ["10.10.2010" "10.10.2010"]]]
      (is (= (t/birth-date dob) expected)))))
