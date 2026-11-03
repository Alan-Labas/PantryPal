export type Zivilo = {
  id: string
  ime: string
  kategorija: string
  enota: string
  opis: string
}

export type Trgovina = {
  id: string
  ime: string
  kraj: string
  opis: string
}

export type ZalogaPrikaz = {
  id: string
  ziviloId: string
  imeZivila: string
  kategorija: string
  opisZivila: string
  kolicina: number
  enota: string
  trgovinaId: string
  imeTrgovine: string
  kraj: string
  datumNakupa: string
  rokUporabe: string | null
}

export type ZalogaOdgovor = {
  zivila: Zivilo[]
  trgovine: Trgovina[]
  postavke: ZalogaPrikaz[]
  filtriranoJson: string
  filtriranoXml: string
}
