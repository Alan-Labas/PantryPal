export type FilterZalogeParams = {
  datum?: string
  trgovinaId?: string
  ziviloId?: string
}

export type OdprtiPodatkiParams = {
  od?: number
  doLeta?: number
  primerjalnoLeto?: number
  kode?: string[]
}

export type LlmObrazecRequest = {
  vrstaObroka: string
  najvecjiCasMin: number
  steviloPorcij: number
  prehranskeOmejitve: string
  poudarek: string
  dodatnaZelja: string
  casBrskalnika: string
  casovniPas: string
  jezikBrskalnika: string
}
