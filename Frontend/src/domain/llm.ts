export type PredlaganRecept = {
  naslov: string
  vrstaObroka: string
  casPripraveMin: number
  steviloPorcij: number
  uporabljenaZivilaIzZaloge: string[]
  manjkajoceSestavine: string[]
  korakiPriprave: string[]
  utemeljitevIzbire: string
}

export type AiOdgovor = {
  povzetekKonteksta: string
  prednostnaZivilaPredRokom: string[]
  recepti: PredlaganRecept[]
  nasvetZaZmanjsanjeOdpadkov: string
}

export type PostavkaZaloge = {
  ime: string
  kategorija: string
  kolicina: number
  enota: string
  rokUporabe: string | null
}

export type LlmZahteva = {
  vrstaObroka: string
  najvecjiCasMin: number
  steviloPorcij: number
  prehranskeOmejitve: string
  poudarek: string
  dodatnaZelja: string
}

export type LlmRezultat = {
  model: string
  casPovprasevanja: string
  zahteva: LlmZahteva
  sestavljenPrompt: string
  odgovor: AiOdgovor
  povprecniCasPripraveMin: number
  steviloUporabljenihZivilIzZaloge: number
}

export type LlmPromptPredogled = {
  prompt: string
  model: string
  imaApiKljuc: boolean
  zaloga: PostavkaZaloge[]
}
