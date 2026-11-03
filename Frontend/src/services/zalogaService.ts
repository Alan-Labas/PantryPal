import type { ZalogaOdgovor } from '../domain/zaloga.ts'
import type { FilterZalogeParams } from '../dto/pantryDto.ts'
import { api } from './api.ts'

export const getZaloga = async (params?: FilterZalogeParams): Promise<ZalogaOdgovor> => {
  const response = await api.get<ZalogaOdgovor>('/zaloga', { params })
  return response.data
}
