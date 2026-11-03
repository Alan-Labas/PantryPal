import { useCallback, useEffect, useState } from 'react'
import { isAxiosError } from 'axios'
import type { ZalogaOdgovor } from '../domain/zaloga.ts'
import type { FilterZalogeParams } from '../dto/pantryDto.ts'
import * as zalogaService from '../services/zalogaService.ts'

export function useZaloga() {
  const [data, setData] = useState<ZalogaOdgovor>({
    zivila: [],
    trgovine: [],
    postavke: [],
    filtriranoJson: '',
    filtriranoXml: '',
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async (params?: FilterZalogeParams) => {
    setLoading(true)
    setError(null)
    try {
      const response = await zalogaService.getZaloga(params)
      setData(response)
    } catch (err) {
      const msg =
        isAxiosError(err) && typeof err.response?.data?.napaka === 'string'
          ? err.response.data.napaka
          : 'Nalaganje zaloge ni uspelo. Preveri, ali zaledni strežnik teče na portu 8080.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  return {
    zivila: data.zivila,
    trgovine: data.trgovine,
    postavke: data.postavke,
    filtriranoJson: data.filtriranoJson,
    filtriranoXml: data.filtriranoXml,
    loading,
    error,
    refresh,
  }
}
