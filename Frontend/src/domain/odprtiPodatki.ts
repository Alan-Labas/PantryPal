export type Kategorija = {
  koda: string
  naziv: string
}

export type Podatek = {
  leto: number
  koda: string
  tone: number | null
}

export type Delez = {
  koda: string
  naziv: string
  tone: number | null
  odstotek: number | null
}

export type Povzetek = {
  leto: number
  skupajTone: number | null
  gospodinjstvaTone: number | null
  delezGospodinjstev: number | null
  spremembaGospodinjstev: number | null
  izvori: Delez[]
}

export type OdprtiPodatkiOdgovor = {
  naslov: string
  enota: string
  vir: string
  opsiUrl: string
  pxUrl: string
  licenca: string
  posodobljeno: string
  pridobljeno: string
  lokalnaKopija: boolean
  opozorilo: string
  vsaLeta: number[]
  leta: number[]
  kategorije: Kategorija[]
  izbraneKode: string[]
  podatki: Podatek[]
  povzetek: Povzetek
}
