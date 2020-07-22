import * as lomakkeenMuokkaus from '../lomakkeenMuokkaus'
import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'
import * as reitit from "../reitit";
import {haeLomakkeenLisaysNappi} from "../lomakkeenMuokkaus";

describe('Hakukohteet -moduuli', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeiden käsittelyä varten', () => {
    it('Navigoi lomakkeen muokkausnäkymään', () => {
      lomakkeenMuokkaus.haeLomakkeenLisaysNappi().should('be.enabled')
    })

    lomakkeenLuonti(() => {
      it('Näyttää hakukohdeet -moduulin', () => {
        lomakkeenMuokkaus.hakukohteet
          .haeOtsikko()
          .should('have.text', 'Hakukohteet')
      })
    })
  })
})
