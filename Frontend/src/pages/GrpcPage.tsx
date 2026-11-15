import { useState } from 'react'
import {
  Activity,
  Filter,
  Minus,
  Play,
  Plus,
  Radio,
  RefreshCw,
  Square,
} from 'lucide-react'
import { useGrpc } from '../hooks/useGrpc.ts'
import {
  buttonClass,
  cardClass,
  inputClass,
  labelClass,
  secondaryButtonClass,
} from '../components/ui.ts'

const btnPrimary = `${buttonClass} !w-auto !text-sm`
const btnSecondary = secondaryButtonClass
const badgeBlue = 'inline-flex items-center rounded-md bg-primary-soft px-2 py-0.5 text-xs font-medium text-primary'
const badgeGreen = 'inline-flex items-center rounded-md bg-emerald-500/15 px-2 py-0.5 text-xs font-medium text-emerald-600 dark:text-emerald-400'
const badgeRed = 'inline-flex items-center rounded-md bg-danger-soft px-2 py-0.5 text-xs font-medium text-danger'

export function GrpcPage() {
  const {
    referencniDatum,
    setReferencniDatum,
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
  } = useGrpc()

  const [izbranaKategorija, setIzbranaKategorija] = useState('Vse')
  const [pragDni, setPragDni] = useState(7)
  const [zakasnitevMs, setZakasnitevMs] = useState(250)
  const [samoKriticni, setSamoKriticni] = useState(false)

  const kategorije = ['Vse', ...(statistika?.razpolozljiveKategorije ?? [])]

  const obSpremembiKategorije = (kat: string) => {
    setIzbranaKategorija(kat)
    void kliciZalogoPoKategoriji(kat)
  }

  const obPosodobitviStatistike = () => {
    void kliciStatistiko(pragDni)
    void kliciZalogoPoKategoriji(izbranaKategorija)
  }

  const zadnjiStream = streamDogodki[streamDogodki.length - 1]
  const napredekOdstotek =
    zadnjiStream && zadnjiStream.skupajVTok > 0
      ? Math.round((zadnjiStream.zaporednaStevilka / zadnjiStream.skupajVTok) * 100)
      : 0

  return (
    <>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl font-bold">gRPC storitve & pretakanje</h1>
          </div>
        </div>
      </div>

      {error && (
        <div
          role="alert"
          className="mb-4 rounded-xl border border-danger bg-danger-soft px-4 py-3 text-sm text-danger"
        >
          {error}
        </div>
      )}


      {/* 1. SKLOP: Referenčni datum + Unary RPC 2 (IzracunajStatistikoZaloge) */}
      <div className={`${cardClass} mb-6`}>
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
          <div>
            <span className={badgeBlue}>Unary RPC #2</span>
            <h2 className="mt-1 text-lg font-semibold">
              Izracunaj Statistiko Zaloge (Statistika Zahtevek → Statistika Odgovor)
            </h2>
          </div>
          <button
            type="button"
            onClick={obPosodobitviStatistike}
            disabled={isLoading}
            className={btnPrimary}
          >
            <RefreshCw size={16} />
            Izvedi gRPC izračun
          </button>
        </div>

        <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <label htmlFor="grpc-ref-datum" className={labelClass}>
              Referenčni datum za izračun rokov
            </label>
            <input
              id="grpc-ref-datum"
              type="date"
              value={referencniDatum}
              onChange={(e) => setReferencniDatum(e.target.value)}
              className={inputClass}
            />
          </div>
          <div>
            <label htmlFor="grpc-prag-dni" className={labelClass}>
              Prag opozorila pred iztekom (dni)
            </label>
            <input
              id="grpc-prag-dni"
              type="number"
              min={1}
              max={60}
              value={pragDni}
              onChange={(e) => setPragDni(Number(e.target.value) || 7)}
              className={inputClass}
            />
          </div>
        </div>

        {statistika && (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
            <div className="rounded-lg border border-border bg-bg p-3">
              <div className="text-xs text-muted">Skupaj zapisov</div>
              <div className="mt-1 text-xl font-bold">{statistika.skupajZapisov}</div>
            </div>
            <div className="rounded-lg border border-border bg-bg p-3">
              <div className="text-xs text-muted">Že poteklo</div>
              <div className="mt-1 text-xl font-bold text-danger">
                {statistika.poteklihZapisov}
              </div>
            </div>
            <div className="rounded-lg border border-border bg-bg p-3">
              <div className="text-xs text-muted">
                Kmalu poteče (≤{statistika.pragDniOpozorila} d)
              </div>
              <div className="mt-1 text-xl font-bold text-primary">
                {statistika.kmaluPoteceZapisov}
              </div>
            </div>
            <div className="rounded-lg border border-border bg-bg p-3">
              <div className="text-xs text-muted">Varnih zapisov</div>
              <div className="mt-1 text-xl font-bold text-success">
                {statistika.varnihZapisov}
              </div>
            </div>
            <div className="rounded-lg border border-border bg-bg p-3">
              <div className="text-xs text-muted">Delež kritičnih</div>
              <div className="mt-1 text-xl font-bold">
                {statistika.delezKriticnihOdstotek.toFixed(1)} %
              </div>
            </div>
          </div>
        )}
      </div>

      {/* 2. SKLOP: Server-Side Streaming RPC #4 (SpremljajKriticnoZalogoStream) */}
      <div className={`${cardClass} mb-6`}>
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
          <div>
            <span className={badgeRed}>Server-Side Streaming RPC #4</span>
            <h2 className="mt-1 text-lg font-semibold">
              Spremljaj Kriticno Zalogo Stream (Stream Zahtevek → stream Kriticna Postavka Dogodek)
            </h2>
            <p className="text-xs text-muted">
              Strežnik prek odprtega gRPC toka postopoma pošilja posamezna sporočila v realnem času.
            </p>
          </div>
          <div className="flex items-center gap-2">
            {!isStreaming ? (
              <button
                type="button"
                onClick={() => zazeniStream(zakasnitevMs, samoKriticni)}
                className={btnPrimary}
              >
                <Play size={16} />
                Zaženi gRPC tok v živo
              </button>
            ) : (
              <button
                type="button"
                onClick={ustaviStream}
                className={btnSecondary}
              >
                <Square size={16} />
                Ustavi tok
              </button>
            )}
          </div>
        </div>

        <div className="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <label htmlFor="grpc-zakasnitev" className={labelClass}>
              Zakasnitev med paketi v toku (ms): {zakasnitevMs} ms
            </label>
            <input
              id="grpc-zakasnitev"
              type="range"
              min={50}
              max={800}
              step={50}
              value={zakasnitevMs}
              onChange={(e) => setZakasnitevMs(Number(e.target.value))}
              className="w-full accent-primary"
            />
          </div>
          <div className="flex items-end pb-1">
            <label className="flex cursor-pointer items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={samoKriticni}
                onChange={(e) => setSamoKriticni(e.target.checked)}
                className="h-4 w-4 accent-primary"
              />
              Pretakaj samo kritične artikle (potekli ali ≤ 14 dni)
            </label>
          </div>
        </div>

        {(isStreaming || streamDogodki.length > 0) && (
          <div className="mb-4">
            <div className="mb-1.5 flex items-center justify-between text-xs text-muted">
              <span className="flex items-center gap-1.5">
                <Radio
                  size={14}
                  className={isStreaming ? 'animate-pulse text-danger' : 'text-success'}
                />
                {isStreaming
                  ? 'Prejemanje gRPC toka v realnem času...'
                  : 'gRPC tok uspešno zaključen'}
              </span>
              <span>
                Prejeto: {streamDogodki.length} / {zadnjiStream?.skupajVTok ?? streamDogodki.length}{' '}
                ({napredekOdstotek} %)
              </span>
            </div>
            <div className="h-2 w-full overflow-hidden rounded-full bg-border">
              <div
                className="h-full bg-primary transition-all duration-200"
                style={{ width: `${napredekOdstotek}%` }}
              />
            </div>
          </div>
        )}

        {streamDogodki.length > 0 && (
          <div className="max-h-72 space-y-2 overflow-y-auto pr-1">
            {streamDogodki.map((d) => {
              const badge =
                d.nujnost === 'POTEKLO' || d.nujnost === 'NUJNO PORABITI'
                  ? badgeRed
                  : d.nujnost === 'OPOZORILO'
                    ? badgeBlue
                    : badgeGreen
              return (
                <div
                  key={d.zaporednaStevilka}
                  className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-border bg-bg px-3.5 py-2.5 text-sm"
                >
                  <div className="flex items-center gap-2.5">
                    <span className="font-mono text-xs text-muted">
                      #{d.zaporednaStevilka}/{d.skupajVTok}
                    </span>
                    <span className={badge}>{d.nujnost}</span>
                    <span className="font-semibold">{d.postavka.imeZivila}</span>
                    <span className="text-xs text-muted">
                      ({d.postavka.kolicina} {d.postavka.enota} · {d.postavka.trgovina})
                    </span>
                  </div>
                  <div className="text-xs text-muted">{d.priporocilo}</div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* 3. SKLOP: Unary RPC #1 (PridobiZalogoPoKategoriji) + Unary RPC #3 (PosodobiKolicino) */}
      <div className={cardClass}>
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <span className={badgeBlue}>Unary RPC #1</span>
              <span className={badgeGreen}>Unary RPC #3</span>
            </div>
            <h2 className="mt-1 text-lg font-semibold">
              Pridobi Zalogo Po Kategoriji & Posodobi Kolicino
            </h2>
            <p className="text-xs text-muted">
              Filtriraj zalogo prek gRPC klica ali v realnem času spremeni količino posameznega zapisa.
            </p>
          </div>
          {zadnjaPosodobitev && (
            <span className={badgeGreen}>
              <Activity size={12} className="mr-1 inline" />
              Posodobljeno: {zadnjaPosodobitev.imeZivila} ({zadnjaPosodobitev.kolicina}{' '}
              {zadnjaPosodobitev.enota})
            </span>
          )}
        </div>

        <div className="mb-4 flex flex-wrap items-center gap-2">
          <span className="flex items-center gap-1 text-xs font-medium text-muted">
            <Filter size={14} /> Kategorija:
          </span>
          {kategorije.map((kat) => (
            <button
              key={kat}
              type="button"
              onClick={() => obSpremembiKategorije(kat)}
              className={
                izbranaKategorija === kat
                  ? `${btnPrimary} !px-3 !py-1.5 !text-xs`
                  : `${btnSecondary} !px-3 !py-1.5 !text-xs`
              }
            >
              {kat}
            </button>
          ))}
        </div>

        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-border text-xs uppercase text-muted">
                <th className="py-2.5 pr-3">ID</th>
                <th className="py-2.5 pr-3">Živilo</th>
                <th className="py-2.5 pr-3">Kategorija</th>
                <th className="py-2.5 pr-3">Trgovina</th>
                <th className="py-2.5 pr-3">Rok uporabe</th>
                <th className="py-2.5 pr-3">Količina</th>
                <th className="py-2.5 text-right">gRPC PosodobiKolicino</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {seznam?.postavke.map((p) => (
                <tr key={p.id} className="hover:bg-bg/60">
                  <td className="py-2.5 pr-3 font-mono text-xs text-muted">{p.id}</td>
                  <td className="py-2.5 pr-3 font-medium">{p.imeZivila}</td>
                  <td className="py-2.5 pr-3">
                    <span className={badgeBlue}>{p.kategorija}</span>
                  </td>
                  <td className="py-2.5 pr-3 text-muted">
                    {p.trgovina} ({p.kraj})
                  </td>
                  <td className="py-2.5 pr-3">
                    {!p.rokUporabe ? (
                      <span className="text-xs text-muted">Brez roka</span>
                    ) : p.jePoteklo ? (
                      <span className={badgeRed}>
                        {p.rokUporabe} ({p.dniDoPoteka} d)
                      </span>
                    ) : (
                      <span className={badgeGreen}>
                        {p.rokUporabe} (+{p.dniDoPoteka} d)
                      </span>
                    )}
                  </td>
                  <td className="py-2.5 pr-3 font-semibold">
                    {p.kolicina} {p.enota}
                  </td>
                  <td className="py-2.5 text-right">
                    <div className="inline-flex items-center gap-1.5">
                      <button
                        type="button"
                        disabled={isLoading}
                        onClick={() =>
                          void kliciPosodobiKolicino(p.id, -1, izbranaKategorija, pragDni)
                        }
                        className={`${btnSecondary} !px-2.5 !py-1 !text-xs`}
                        title="Zmanjšaj količino za 1 prek gRPC"
                      >
                        <Minus size={13} /> 1
                      </button>
                      <button
                        type="button"
                        disabled={isLoading}
                        onClick={() =>
                          void kliciPosodobiKolicino(p.id, 1, izbranaKategorija, pragDni)
                        }
                        className={`${btnPrimary} !px-2.5 !py-1 !text-xs`}
                        title="Povečaj količino za 1 prek gRPC"
                      >
                        <Plus size={13} /> 1
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}
