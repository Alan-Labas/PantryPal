import { useCallback, useEffect, useState } from 'react'
import { isAxiosError } from 'axios'
import type { PorociloZajema, Recept } from '../domain/recept.ts'
import * as receptiService from '../services/receptiService.ts'

export function useRecepti() {
  const [recepti, setRecepti] = useState<Recept[]>([])
  const [porocilo, setPorocilo] = useState<PorociloZajema>({})
  const [loading, setLoading] = useState(true)
  const [scraping, setScraping] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await receptiService.getRecepti()
      setRecepti(data.recepti ?? [])
      setPorocilo(data.porocilo ?? {})
    } catch (err) {
      const msg =
        isAxiosError(err) && typeof err.response?.data?.napaka === 'string'
          ? err.response.data.napaka
          : 'Branje zbirke receptov ni uspelo.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const zazeniParser = useCallback(async (stevilo: number) => {
    setScraping(true)
    setError(null)
    try {
      const data = await receptiService.zajemiRecepte(stevilo)
      setRecepti(data.recepti ?? [])
      setPorocilo(data.porocilo ?? {})
    } catch (err) {
      const msg =
        isAxiosError(err) && typeof err.response?.data?.napaka === 'string'
          ? err.response.data.napaka
          : 'Spletni zajem receptov ni uspel.'
      setError(msg)
    } finally {
      setScraping(false)
    }
  }, [])

  return { recepti, porocilo, loading, scraping, error, refresh, zazeniParser }
}
