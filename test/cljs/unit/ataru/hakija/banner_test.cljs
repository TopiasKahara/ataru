(ns ataru.hakija.banner-test
  (:require [cljs.test :refer-macros [deftest is]]
            [clojure.string :refer [includes?]]
            [re-frame.core :refer [dispatch subscribe]]
            [ataru.hakija.banner :refer [hakuaika-left-text]]
            [ataru.util :as util]))

(deftest correctly-shows-remaining-time
  (defn invoke-and-verify [hours minutes substring]
    (let [seconds-left (+ (* hours 3600) (* minutes 60))
          actual (hakuaika-left-text seconds-left :fi)
          text   (second actual)]
      (is (includes? text substring)))
    )
  (invoke-and-verify 0 14 "Hakuaikaa jäljellä alle 15 min")
  (invoke-and-verify 0 15 "Hakuaikaa jäljellä alle 30 min")
  (invoke-and-verify 0 29 "Hakuaikaa jäljellä alle 30 min")
  (invoke-and-verify 0 30 "Hakuaikaa jäljellä alle 45 min")
  (invoke-and-verify 0 44 "Hakuaikaa jäljellä alle 45 min")
  (invoke-and-verify 0 45 "Hakuaikaa jäljellä alle tunti")
  (invoke-and-verify 0 59 "Hakuaikaa jäljellä alle tunti")
  (invoke-and-verify 1 00 "Hakuaikaa jäljellä alle vuorokausi")
  (invoke-and-verify 23 59 "Hakuaikaa jäljellä alle vuorokausi")
  (invoke-and-verify 0 -1 "Hakuaika on päättynyt")
  (is (nil? (hakuaika-left-text nil :fi)))
  (is (nil? (hakuaika-left-text (* 24 60 60) :fi))))

