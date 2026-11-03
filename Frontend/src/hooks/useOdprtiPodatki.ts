import { useCallback, useEffect, useState } from 'react'
import { isAxiosError } from 'axios'
import type { OdprtiPodatkiOdgovor } from '../domain/odprtiPodatki.ts'
import type { OdprtiPodatkiParams } from '../dto/pantryDto.ts'
import * as odprtiPodatkiService from '../services/odprtiPodatkiService.ts'

export function useOdprtiPodatki() {
  const [podatki, setPodatki] = useState<OdprtiPodatkiOdgovor | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async (params?: OdprtiPodatkiParams) => {
    setLoading(true)
    setError(null)
    try {
      const response = await odprtiPodatkiService.getOdprtiPodatki(params)
      setPodatki(response)
    } catch (err) {
      const msg =
        isAxiosError(err) && typeof err.response?.data?.napaka === 'string'
          ? err.response.data.napaka
          : 'Nalaganje odprtih podatkov PCAXIS ni uspelo.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh({ od: 2013, doLeta: 2025, primerjalnoLeto: 2025, kode: ['1', '5'] })
  }, [refresh])

  return { podatki, loading, error, refresh }
}
