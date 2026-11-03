export type Recept = {
  id: string
  naslov: string
  virUrl: string
  kategorija: string
  porcije: string
  skupniCas: string
  sestavine: string[]
  datumZajema: string
}

export type PorociloZajema = {
  vir?: string
  zacetek?: string
  konec?: string
  zahtevano?: number
  prebraneStrani?: number
  poskusi?: number
  shranjeno?: number
  razlogZakljucka?: string
}

export type ReceptiOdgovor = {
  recepti: Recept[]
  porocilo: PorociloZajema
}
