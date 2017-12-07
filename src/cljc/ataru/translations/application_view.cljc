(ns ataru.translations.application-view)

(def application-view-translations
  {:application-period                {:fi "Hakuaika"
                                       :sv "Ansökningstid"
                                       :en "Application period"}
   :not-within-application-period     {:fi "hakuaika ei ole käynnissä"
                                       :sv "inte inom ansökningstiden"
                                       :en "application period currently not ongoing"}
   :application-processed-cant-modify {:fi "Tämä hakemus on käsitelty eikä ole enää muokattavissa"
                                       :sv "Denna ansökan har behandlats och kan inte längre bearbetas"
                                       :en "This application has been processed and can no longer be modified"}
   :continuous-period                 {:fi "Jatkuva haku"
                                       :sv "kontinuerlig ansökningstid"
                                       :en "Continuous application period"}
   :add-row                           {:fi "Lisää rivi"
                                       :sv "Lägg till rad"
                                       :en "Add row"}
   :remove-row                        {:fi "Poista rivi"
                                       :sv "Ta bort rad"
                                       :en "Remove row"}
   :add-more                          {:fi "Lisää..."
                                       :sv "Lägg till..."
                                       :en "Add more..."}
   :add-more-questiongroup            {:fi "Lisää"
                                       :sv "Lägg till"
                                       :en "Add more"}
   :add-attachment                    {:fi "Lisää liite..."
                                       :en "Upload attachment..."
                                       :sv "Ladda upp bilagan..."}
   :feedback-header                   {:fi "Hei, kerro vielä mitä pidit hakulomakkeesta!"
                                       :en "Hi! Care to take a moment to rate our application form?"
                                       :sv "Hej, berätta ännu vad du tyckte om ansökningsblanketten?"}
   :feedback-disclaimer               {:fi "Yhteystietojasi ei käytetä tai yhdistetä palautteen tietoihin."
                                       :en "Your personal information is not sent or associated with the feedback given."
                                       :sv "Dina kontaktuppgifter används inte och kopplas inte heller ihop med responsuppgifterna."}
   :feedback-ratings                  {:fi {1 "Huono"
                                            2 "Välttävä"
                                            3 "Tyydyttävä"
                                            4 "Hyvä"
                                            5 "Kiitettävä"}
                                       :en {1 "Poor"
                                            2 "Passable"
                                            3 "OK"
                                            4 "Good"
                                            5 "Excellent"}
                                       :sv {1 "Dålig"
                                            2 "Försvarlig"
                                            3 "Nöjaktig"
                                            4 "Bra"
                                            5 "Berömlig"}}
   :feedback-text-placeholder         {:fi "Anna halutessasi kommentteja hakulomakkeesta."
                                       :en "Feel free to also share your comments regarding the application form."
                                       :sv "Om du vill kan du ge kommentarer om ansökningsblanketten."}
   :feedback-send                     {:fi "Lähetä palaute"
                                       :en "Send feedback"
                                       :sv "Skicka respons"}
   :feedback-thanks                   {:fi "Kiitos palautteestasi!"
                                       :en "Thank you for your feedback!"
                                       :sv "Tack för din respons!"}
   :page-title                        {:fi "Opintopolku – hakulomake"
                                       :en "Studyinfo – application form"
                                       :sv "Studieinfo – ansökningsblankett"}
   :application-sending               {:fi "Hakemusta lähetetään"
                                       :sv "Ansökan skickas"
                                       :en "The application is being sent"}
   :application-confirmation          {:fi "Saat vahvistuksen sähköpostiisi"
                                       :sv "Du får en bekräftelse till din e-post"
                                       :en "Confirmation email will be sent to the email address you've provided"}
   :application-sent                  {:fi "Hakemus lähetetty"
                                       :sv "Ansökan har skickats"
                                       :en "The application has been sent"}
   :modifications-saved               {:fi "Muutokset tallennettu"
                                       :sv "Ändringarna har sparats"
                                       :en "The modifications have been saved"}
   :application-hakija-edit-text      {:fi "LÄHETÄ MUUTOKSET"
                                       :sv "SCICKA FÖRÄNDRINGAR"
                                       :en "SEND MODIFICATIONS"}
   :application-virkailija-edit-text  {:fi "TALLENNA MUUTOKSET"
                                       :sv "SPARA FÖRÄNDRINGAR"
                                       :en "SAVE MODIFICATIONS"}
   :hakija-new-text                   {:fi "LÄHETÄ HAKEMUS"
                                       :sv "SKICKA ANSÖKAN"
                                       :en "SEND APPLICATION"}
   :check-answers                     {:fi ["Tarkista " " tietoa"]
                                       :sv ["Kontrollera " " uppgifter"]
                                       :en ["Check " " answers"]}
   :file-size-info                    {:fi "Tiedoston maksimikoko on 10 MB"
                                       :en "Maximum file size is 10 MB"
                                       :sv "Den maximala filstorleken är 10 MB"}})
