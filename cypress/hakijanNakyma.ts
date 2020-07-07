import * as tekstikentta from './tekstikentta'
import * as dropdown from './dropdown'

import Chainable = Cypress.Chainable

interface OppiaineenArvosanat {
  oppiaine: string
  arvosana: string
  oppimaara?: string
  index: number
}

export const haeLomakkeenNimi = () =>
  cy.get('[data-test-id=application-header-label]:visible')

export const arvosanat = {
  asetaOppiaineenArvosanat: ({
    oppiaine,
    arvosana,
    oppimaara,
    index,
  }: OppiaineenArvosanat) =>
    dropdown
      .setDropdownValue(
        `oppiaineen-arvosana-${oppiaine}-arvosana-${index}`,
        `arvosana-${oppiaine}-${arvosana}`
      )
      .then((c) =>
        oppimaara
          ? dropdown.setDropdownValue(
              `oppiaineen-arvosana-${oppiaine}-oppimaara-${index}`,
              oppimaara
            )
          : c
      ),

  haeValinnaisaineLinkki: ({
    oppiaine,
    index,
    poisKaytosta,
  }: {
    oppiaine: string
    index: number
    poisKaytosta: boolean
  }) =>
    cy.get(
      `${
        poisKaytosta ? 'span' : 'a'
      }[data-test-id=oppiaineen-arvosana-${oppiaine}-lisaa-valinnaisaine-linkki-0-${
        index === 0 ? 'lisaa' : 'poista'
      }]`
    ),

  lisaaValinnaisaine: ({ oppiaine }: { oppiaine: string }) =>
    arvosanat
      .haeValinnaisaineLinkki({ oppiaine, index: 0, poisKaytosta: false })
      .click(),

  haeOppiaineenArvosanaRivi: ({ oppiaine }: { oppiaine: string }) =>
    cy.get(`[data-test-id=oppiaineen-arvosana-${oppiaine}]`),

  tarkistaLukunakymanOppiaine: ({
    oppiaine,
    oppimaara,
    arvosana,
    index,
  }: {
    oppiaine: string
    oppimaara?: string
    arvosana: string
    index: number
  }) =>
    cy
      .get(
        `[data-test-id=oppiaineen-arvosana-readonly-${oppiaine}-arvosana-${index}]`
      )
      .should('have.text', arvosana)
      .then((c) =>
        oppiaine === 'A'
          ? cy
              .get(
                `[data-test-id=oppiaineen-arvosana-readonly-${oppiaine}-oppimaara-${index}]`
              )
              .should('have.text', oppimaara)
          : c
      ),
}

const syota = <T>(
  elementti: Chainable<T>,
  teksti: string
): (() => Chainable<T>) => () =>
  tekstikentta.syotaTekstiTarkistamatta(elementti, teksti)

const syotaTurvallisesti = <T>(
  elementti: Chainable<T>,
  teksti: string
): (() => Chainable<T>) => () => tekstikentta.syotaTeksti(elementti, teksti)

export const henkilotiedot = {
  etunimi: () => cy.get('[data-test-id=first-name-input]'),
  sukunimi: () => cy.get('[data-test-id=last-name-input]'),
  henkilotunnus: () => cy.get('[data-test-id=ssn-input]'),
  sahkoposti: () => cy.get('[data-test-id=email-input]'),
  sahkopostitoisto: () => cy.get('[data-test-id=verify-email-input]'),
  matkapuhelin: () => cy.get('[data-test-id=phone-input]'),
  katusoite: () => cy.get('[data-test-id=address-input]'),
  postinumero: () => cy.get('[data-test-id=postal-code-input]'),
  postitoimipaikka: () => cy.get('[data-test-id=postal-office-input]'),
  kotikunta: () => cy.get('[data-test-id=home-town-input]'),

  taytaTiedot: () => {
    return tekstikentta
      .syotaTekstiTarkistamatta(henkilotiedot.etunimi(), 'Frank Zacharias')
      .then(syota(henkilotiedot.sukunimi(), 'Testerberg'))
      .then(syota(henkilotiedot.henkilotunnus(), '160600A999C'))
      .then(syotaTurvallisesti(henkilotiedot.sahkoposti(), 'f.t@ex.com'))
      .then(syotaTurvallisesti(henkilotiedot.sahkopostitoisto(), 'f.t@ex.com'))
      .then(syota(henkilotiedot.matkapuhelin(), '0401234567'))
      .then(syota(henkilotiedot.katusoite(), 'Yliopistonkatu 4'))
      .then(syota(henkilotiedot.postinumero(), '00100'))
      .then(() => henkilotiedot.kotikunta().select('Forssa'))
  },
}

export const klikkaa = (elementinTeksti: string) =>
  cy.get(`label:contains(${elementinTeksti})`).click({ multiple: true })

export const lahetaHakemus = () =>
  cy.get('[data-test-id=send-application-button]').click()

export const painaOkPalautenakymassa = () =>
  cy.get('[data-test-id=send-feedback-button]').click()

export const suljePalaute = () =>
  cy.get('[data-test-id=close-feedback-form-button]').click()