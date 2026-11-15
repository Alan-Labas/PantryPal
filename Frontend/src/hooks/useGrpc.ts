import { useCallback, useEffect, useRef, useState } from 'react'
import type {
  KriticnaPostavkaDogodek,
  PostavkaZalogeMsg,
  ProtoInfo,
  SeznamZalogeOdgovor,
  StatistikaOdgovor,
} from '../domain/grpc.ts'
import { grpcService } from '../services/grpcService.ts'

export function useGrpc() {
  const [referencniDatum, setReferencniDatum] = useState('2026-10-27')
  const [protoInfo, setProtoInfo] = useState<ProtoInfo | null>(null)
  const [seznam, setSeznam] = useState<SeznamZalogeOdgovor | null>(null)
  const [statistika, setStatistika] = useState<StatistikaOdgovor | null>(null)
  const [zadnjaPosodobitev, setZadnjaPosodobitev] = useState<PostavkaZalogeMsg | null>(null)
  const [streamDogodki, setStreamDogodki] = useState<KriticnaPostavkaDogodek[]>([])
  const [isStreaming, setIsStreaming] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const stopStreamRef = useRef<(() => void) | null>(null)

  const naloziOsnovo = useCallback(async (kat = 'Vse', datum = referencniDatum, prag = 7) => {
    setIsLoading(true)
    setError(null)
    try {
      const [proto, zalogaOdg, statOdg] = await Promise.all([
        grpcService.getProto(),
        grpcService.pridobiZalogoPoKategoriji(kat, datum),
        grpcService.izracunajStatistikoZaloge(datum, prag),
      ])
      setProtoInfo(proto)
      setSeznam(zalogaOdg)
      setStatistika(statOdg)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Napaka pri gRPC komunikaciji')
    } finally {
      setIsLoading(false)
    }
  }, [referencniDatum])

  useEffect(() => {
    void naloziOsnovo('Vse', '2026-10-27', 7)
    return () => {
      stopStreamRef.current?.()
    }
  }, [naloziOsnovo])

  const kliciZalogoPoKategoriji = async (kategorija: string) => {
    setIsLoading(true)
    setError(null)
    try {
      const odg = await grpcService.pridobiZalogoPoKategoriji(kategorija, referencniDatum)
      setSeznam(odg)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Napaka pri RPC PridobiZalogoPoKategoriji')
    } finally {
      setIsLoading(false)
    }
  }

  const kliciStatistiko = async (pragDni: number) => {
    setIsLoading(true)
    setError(null)
    try {
      const odg = await grpcService.izracunajStatistikoZaloge(referencniDatum, pragDni)
      setStatistika(odg)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Napaka pri RPC IzracunajStatistikoZaloge')
    } finally {
      setIsLoading(false)
    }
  }

  const kliciPosodobiKolicino = async (zapisId: string, sprememba: number, aktivnaKategorija: string, pragDni: number) => {
    setIsLoading(true)
    setError(null)
    try {
      const posodobljena = await grpcService.posodobiKolicino(zapisId, sprememba, referencniDatum)
      setZadnjaPosodobitev(posodobljena)
      const [zalogaOdg, statOdg] = await Promise.all([
        grpcService.pridobiZalogoPoKategoriji(aktivnaKategorija, referencniDatum),
        grpcService.izracunajStatistikoZaloge(referencniDatum, pragDni),
      ])
      setSeznam(zalogaOdg)
      setStatistika(statOdg)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Napaka pri RPC PosodobiKolicino')
    } finally {
      setIsLoading(false)
    }
  }

  const zazeniStream = (zakasnitevMs: number, samoKriticni: boolean) => {
    stopStreamRef.current?.()
    setStreamDogodki([])
    setIsStreaming(true)
    setError(null)

    stopStreamRef.current = grpcService.zazeniStreamKriticneZaloge(
      referencniDatum,
      zakasnitevMs,
      samoKriticni,
      (dogodek) => {
        setStreamDogodki((prev) => [...prev, dogodek])
      },
      () => {
        setIsStreaming(false)
      },
      (napaka) => {
        setIsStreaming(false)
        setError(napaka)
      },
    )
  }

  const ustaviStream = () => {
    stopStreamRef.current?.()
    setIsStreaming(false)
  }

  return {
    referencniDatum,
    setReferencniDatum,
    protoInfo,
    seznam,
    statistika,
    zadnjaPosodobitev,
    streamDogodki,
    isStreaming,
    isLoading,
    error,
    kliciZalogoPoKategoriji,
    kliciStatistiko,
    kliciPosodobiKolicino,
    zazeniStream,
    ustaviStream,
  }
}
