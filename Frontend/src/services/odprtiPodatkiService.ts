import type { OdprtiPodatkiOdgovor } from '../domain/odprtiPodatki.ts'
import type { OdprtiPodatkiParams } from '../dto/pantryDto.ts'
import { api } from './api.ts'

export const getOdprtiPodatki = async (params?: OdprtiPodatkiParams): Promise<OdprtiPodatkiOdgovor> => {
  const searchParams = new URLSearchParams()
  if (params?.od != null) searchParams.set('od', String(params.od))
  if (params?.doLeta != null) searchParams.set('doLeta', String(params.doLeta))
  if (params?.primerjalnoLeto != null) searchParams.set('primerjalnoLeto', String(params.primerjalnoLeto))
  if (params?.kode && params.kode.length > 0) {
    params.kode.forEach((k) => searchParams.append('kode', k))
  }
  const response = await api.get<OdprtiPodatkiOdgovor>(`/odprti-podatki?${searchParams.toString()}`)
  return response.data
}
