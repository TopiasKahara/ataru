(ns ataru.applications.excel-export-spec
  (:require [ataru.applications.application-store :as application-store]
            [ataru.applications.excel-export :as j2ee]
            [ataru.fixtures.excel-fixtures :as fixtures]
            [ataru.forms.form-store :as form-store]
            [speclj.core :refer :all])
  (:import [java.io FileOutputStream File]
           [java.util UUID]
           [org.apache.poi.ss.usermodel WorkbookFactory]))

(defn- verify-row
  [sheet row-num expected-values]
  (let [row (.getRow sheet row-num)]
    (if (nil? expected-values)
      (should-be-nil row)
      (do (should-not-be-nil row)
          (doseq [col-idx (range (count expected-values))]
            (let [cell     (.getCell row col-idx)
                  expected (nth expected-values col-idx)]
              (if-not (nil? expected)
                (do (should-not-be-nil cell)
                    (should= (nth expected-values col-idx) (.getStringCellValue cell)))
                (should-be-nil cell))))
          (should-be-nil (.getCell row (count expected-values)))))))

(defn- verify-pane-information
  [sheet]
  (let [info (.getPaneInformation sheet)]
    (should (.isFreezePane info))
    (should= 1 (.getHorizontalSplitPosition info))
    (should= 0 (.getVerticalSplitPosition info))
    (should= 1 (.getHorizontalSplitTopRow info))
    (should= 0 (.getVerticalSplitLeftColumn info))))

(defmacro with-excel [bindings & body]
  `(let [~(first bindings) (File/createTempFile (str "excel-" (UUID/randomUUID)) ".xlsx")]
     (try
       (with-open [output# (FileOutputStream. (.getPath ~(first bindings)))]
         (->> (j2ee/export-applications ~(second bindings))
              (.write output#)))
       ~@body
       (finally
         (.delete ~(first bindings))))))

(describe "excel export"
  (tags :unit :excel)

  (around [spec]
    (with-redefs [application-store/get-application-review (fn [& _] fixtures/application-review)
                  form-store/fetch-by-id (fn [id]
                                           (case id
                                             123 fixtures/form))
                  form-store/fetch-by-key (fn [key]
                                            (case key
                                              "form_123_key" fixtures/form))]
      (spec)))



  (it "should export applications for a form without hakukohde or haku"
    (with-excel [file [fixtures/application-for-form]]
      (let [workbook          (WorkbookFactory/create file)
            metadata-sheet    (.getSheetAt workbook 0)
            application-sheet (.getSheetAt workbook 1)]
        (verify-row metadata-sheet 0 ["Nimi" "Id" "Tunniste" "Viimeksi muokattu" "Viimeinen muokkaaja"])
        (verify-row metadata-sheet 1 ["Form name" "123" "form_123_key" "2016-06-14 15:34:56" "SEPPO PAPUNEN"])
        (verify-row metadata-sheet 2 nil)
        (verify-row application-sheet 0 ["Id" "Lähetysaika" "Tila" "Kysymys 1" "Kysymys 2" "Muistiinpanot" "Pisteet"])
        (verify-row application-sheet 1 ["application_9432_key" "2016-06-15 15:34:56" "Käsittelemättä" "Vastaus 1" "Vastaus 2" "Some notes about the applicant"])
        (verify-row application-sheet 2 nil)
        (verify-pane-information application-sheet)))))
