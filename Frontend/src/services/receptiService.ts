import type { ReceptiOdgovor } from '../domain/recept.ts'
import { api } from './api.ts'

export const getRecepti = async (): Promise<ReceptiOdgovor> => {
  const response = await api.get<ReceptiOdgovor>('/recepti')
  return response.data
}

export const zajemiRecepte = async (stevilo: number): Promise<ReceptiOdgovor> => {
  const response = await api.post<ReceptiOdgovor>('/recepti/zajemi', { stevilo })
  return response.data
}
