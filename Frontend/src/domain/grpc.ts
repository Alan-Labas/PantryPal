export interface PostavkaZalogeMsg {
  id: string
  ziviloId: string
  imeZivila: string
  kategorija: string
  kolicina: number
  enota: string
  trgovina: string
  kraj: string
  datumNakupa: string
  rokUporabe: string
  dniDoPoteka: number
  jePoteklo: boolean
}

export interface SeznamZalogeOdgovor {
  izbranaKategorija: string
  skupajZapisov: number
  postavke: PostavkaZalogeMsg[]
}

export interface StatistikaOdgovor {
  referencniDatum: string
  pragDniOpozorila: number
  skupajZapisov: number
  poteklihZapisov: number
  kmaluPoteceZapisov: number
  varnihZapisov: number
  brezRokaZapisov: number
  delezKriticnihOdstotek: number
  razpolozljiveKategorije: string[]
}

export interface KriticnaPostavkaDogodek {
  zaporednaStevilka: number
  skupajVTok: number
  nujnost: string
  priporocilo: string
  casOddaje: string
  postavka: PostavkaZalogeMsg
}

export interface ProtoInfo {
  imeDatoteke: string
  grpcPort: number
  vsebina: string
}
