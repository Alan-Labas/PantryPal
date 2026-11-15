import type {
  KriticnaPostavkaDogodek,
  PostavkaZalogeMsg,
  ProtoInfo,
  SeznamZalogeOdgovor,
  StatistikaOdgovor,
} from '../domain/grpc.ts'
import { api } from './api.ts'

export const grpcService = {
  async getProto(): Promise<ProtoInfo> {
    const response = await api.get<ProtoInfo>('/grpc/proto')
    return response.data
  },

  async pridobiZalogoPoKategoriji(
    kategorija: string,
    referencniDatum: string,
  ): Promise<SeznamZalogeOdgovor> {
    const response = await api.get<SeznamZalogeOdgovor>('/grpc/kategorija', {
      params: { kategorija, referencniDatum },
    })
    return response.data
  },

  async izracunajStatistikoZaloge(
    referencniDatum: string,
    pragDni: number,
  ): Promise<StatistikaOdgovor> {
    const response = await api.get<StatistikaOdgovor>('/grpc/statistika', {
      params: { referencniDatum, pragDni },
    })
    return response.data
  },

  async posodobiKolicino(
    zapisId: string,
    spremembaKolicine: number,
    referencniDatum: string,
  ): Promise<PostavkaZalogeMsg> {
    const response = await api.post<PostavkaZalogeMsg>('/grpc/kolicina', {
      zapisId,
      spremembaKolicine,
      referencniDatum,
    })
    return response.data
  },

  zazeniStreamKriticneZaloge(
    referencniDatum: string,
    zakasnitevMs: number,
    samoKriticni: boolean,
    onDogodek: (dogodek: KriticnaPostavkaDogodek) => void,
    onKonec: () => void,
    onNapaka: (err: string) => void,
  ): () => void {
    const params = new URLSearchParams()
    if (referencniDatum) params.set('referencniDatum', referencniDatum)
    params.set('zakasnitevMs', String(zakasnitevMs))
    params.set('samoKriticni', String(samoKriticni))

    const source = new EventSource(`/api/grpc/stream?${params.toString()}`)

    source.addEventListener('kriticna-postavka', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data) as KriticnaPostavkaDogodek
        onDogodek(data)
      } catch {
        // ignore malformed chunk
      }
    })

    source.addEventListener('konec', () => {
      source.close()
      onKonec()
    })

    source.onerror = () => {
      source.close()
      onNapaka('Povezava s strežniškim gRPC tokom je bila prekinjena.')
    }

    return () => {
      source.close()
    }
  },
}
