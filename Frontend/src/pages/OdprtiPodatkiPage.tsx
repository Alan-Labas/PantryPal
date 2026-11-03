import { useState, type FormEvent } from 'react'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js'
import { Bar, Line } from 'react-chartjs-2'
import { AlertCircle, BarChart3, RefreshCw } from 'lucide-react'
import { ThemeToggle } from '../components/ThemeToggle.tsx'
import { buttonClass, cardClass, inputClass, labelClass } from '../components/ui.ts'
import { useOdprtiPodatki } from '../hooks/useOdprtiPodatki.ts'

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
)

const BARVE_SERIJ: Record<string, string> = {
  '1': '#4a7dbd',
  '2': '#d8ad68',
  '3': '#4f9d69',
  '4': '#c25e5e',
  '5': '#7c6fd8',
}

const fieldClass = 'flex min-w-0 flex-col gap-1.5'

export function OdprtiPodatkiPage() {
  const { podatki, loading, error, refresh } = useOdprtiPodatki()
  const [od, setOd] = useState(2013)
  const [doLeta, setDoLeta] = useState(2025)
  const [primerjalnoLeto, setPrimerjalnoLeto] = useState(2025)
  const [izbraneKode, setIzbraneKode] = useState<string[]>(['1', '5'])

  const preklopiKodo = (koda: string) => {
    setIzbraneKode((prej) => {
      if (prej.includes(koda)) {
        return prej.length > 1 ? prej.filter((k) => k !== koda) : prej
      }
      return [...prej, koda]
    })
  }

  const onApply = async (e: FormEvent) => {
    e.preventDefault()
    const veljavnoLeto =
      primerjalnoLeto < od || primerjalnoLeto > doLeta ? doLeta : primerjalnoLeto
    setPrimerjalnoLeto(veljavnoLeto)
    await refresh({ od, doLeta, primerjalnoLeto: veljavnoLeto, kode: izbraneKode })
  }

  const crtniPodatki = () => {
    if (!podatki) return { labels: [], datasets: [] }
    const mapa = new Map<string, number | null>()
    podatki.podatki.forEach((p) => mapa.set(`${p.koda}:${p.leto}`, p.tone))

    return {
      labels: podatki.leta,
      datasets: podatki.izbraneKode.map((koda) => {
        const kat = podatki.kategorije.find((k) => k.koda === koda)
        const barva = BARVE_SERIJ[koda] ?? '#4a7dbd'
        return {
          label: kat ? kat.naziv : koda,
          data: podatki.leta.map((y) => mapa.get(`${koda}:${y}`) ?? null),
          borderColor: barva,
          backgroundColor: barva,
          tension: 0.25,
          pointRadius: 4,
        }
      }),
    }
  }

  const stolpcniPodatki = () => {
    if (!podatki) return { labels: [], datasets: [] }
    const izvori = podatki.povzetek.izvori
    return {
      labels: izvori.map((i) => i.naziv),
      datasets: [
        {
          label: `Odpadna hrana v letu ${podatki.povzetek.leto} (tone)`,
          data: izvori.map((i) => i.tone),
          backgroundColor: izvori.map((i) => BARVE_SERIJ[i.koda] ?? '#4a7dbd'),
          borderRadius: 6,
        },
      ],
    }
  }

  const vsaLeta = podatki?.vsaLeta ?? [
    2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020, 2021, 2022, 2023, 2024, 2025,
  ]

  return (
    <>
      <div className="mb-6 flex items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Odprti podatki Slovenije</h1>
          <p className="mt-1 text-[15px] text-muted">
            Dinamični prenos izvorne datoteke <code>2780705S.PX</code> s portala SURS/OPSI, obdelava
            in grafična vizualizacija s knjižnico <strong>Chart.js</strong>
          </p>
        </div>
        <ThemeToggle />
      </div>

      {error && (
        <div
          className="mb-4 flex items-center gap-2 rounded-lg bg-danger-soft px-3.5 py-3 text-sm text-danger"
          role="alert"
        >
          <AlertCircle size={18} className="shrink-0" />
          {error}
        </div>
      )}

      <form onSubmit={onApply} className={cardClass}>
        <h2 className="mb-3 flex items-center gap-2 text-[17px] font-semibold">
          <BarChart3 size={18} className="text-primary" />
          Filtri in agregacija PC-Axis podatkov
        </h2>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
          <div className={fieldClass}>
            <label htmlFor="px-od" className={labelClass}>
              Začetno leto obdobja
            </label>
            <select
              id="px-od"
              className={inputClass}
              value={od}
              onChange={(e) => setOd(Number(e.target.value))}
            >
              {vsaLeta.map((y) => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
            </select>
          </div>

          <div className={fieldClass}>
            <label htmlFor="px-do" className={labelClass}>
              Končno leto obdobja
            </label>
            <select
              id="px-do"
              className={inputClass}
              value={doLeta}
              onChange={(e) => setDoLeta(Number(e.target.value))}
            >
              {vsaLeta.map((y) => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
            </select>
          </div>

          <div className={fieldClass}>
            <label htmlFor="px-leto" className={labelClass}>
              Primerjalno leto za izvore
            </label>
            <select
              id="px-leto"
              className={inputClass}
              value={primerjalnoLeto}
              onChange={(e) => setPrimerjalnoLeto(Number(e.target.value))}
            >
              {vsaLeta
                .filter((y) => y >= od && y <= doLeta)
                .map((y) => (
                  <option key={y} value={y}>
                    {y}
                  </option>
                ))}
            </select>
          </div>
        </div>

        <div className="mt-4">
          <span className={labelClass}>Izvori za časovni prikaz (Chart.js črtni graf):</span>
          <div className="mt-2 flex flex-wrap gap-2.5">
            {(podatki?.kategorije ?? []).map((k) => {
              const checked = izbraneKode.includes(k.koda)
              return (
                <label
                  key={k.koda}
                  className={`inline-flex cursor-pointer items-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition-colors ${
                    checked
                      ? 'border-primary bg-primary-soft text-primary'
                      : 'border-border bg-surface text-muted'
                  }`}
                >
                  <input
                    type="checkbox"
                    className="accent-primary"
                    checked={checked}
                    onChange={() => preklopiKodo(k.koda)}
                  />
                  {k.naziv}
                </label>
              )
            })}
          </div>
        </div>

        <div className="mt-4">
          <button type="submit" className={`${buttonClass} sm:w-auto`} disabled={loading}>
            <RefreshCw size={17} className={loading ? 'animate-spin' : ''} />
            {loading ? 'Pridobivam .PX datoteko s SURS…' : 'Osveži podatke s SURS in posodobi grafe'}
          </button>
        </div>
      </form>

      {podatki && (
        <>
          <div className="mt-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
            <div className={cardClass}>
              <p className="text-xs font-semibold uppercase text-muted">
                Skupaj ({podatki.povzetek.leto})
              </p>
              <p className="mt-1 text-xl font-bold text-primary">
                {Math.round(podatki.povzetek.skupajTone ?? 0).toLocaleString('sl-SI')} t
              </p>
            </div>
            <div className={cardClass}>
              <p className="text-xs font-semibold uppercase text-muted">
                Gospodinjstva ({podatki.povzetek.leto})
              </p>
              <p className="mt-1 text-xl font-bold text-primary">
                {Math.round(podatki.povzetek.gospodinjstvaTone ?? 0).toLocaleString('sl-SI')} t
              </p>
            </div>
            <div className={cardClass}>
              <p className="text-xs font-semibold uppercase text-muted">Delež gospodinjstev</p>
              <p className="mt-1 text-xl font-bold text-success">
                {podatki.povzetek.delezGospodinjstev != null
                  ? `${podatki.povzetek.delezGospodinjstev.toFixed(1)} %`
                  : 'Ni podatka'}
              </p>
            </div>
            <div className={cardClass}>
              <p className="text-xs font-semibold uppercase text-muted">
                Medletna sprememba ({podatki.povzetek.leto - 1}→{podatki.povzetek.leto})
              </p>
              <p className="mt-1 text-xl font-bold text-fg">
                {podatki.povzetek.spremembaGospodinjstev != null
                  ? `${podatki.povzetek.spremembaGospodinjstev > 0 ? '+' : ''}${podatki.povzetek.spremembaGospodinjstev.toFixed(1)} %`
                  : 'Ni podatka'}
              </p>
            </div>
          </div>

          <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-2">
            <div className={cardClass}>
              <h2 className="mb-1 text-[17px] font-semibold">
                Gibanje odpadne hrane ({podatki.leta[0]}–{podatki.leta[podatki.leta.length - 1]})
              </h2>
              <p className="mb-4 text-xs text-muted">
                Črtni graf Chart.js • Vir: {podatki.vir} ({podatki.enota})
              </p>
              <Line
                data={crtniPodatki()}
                options={{
                  responsive: true,
                  plugins: { legend: { position: 'bottom' } },
                }}
              />
            </div>

            <div className={cardClass}>
              <h2 className="mb-1 text-[17px] font-semibold">
                Struktura po izvorih v letu {podatki.povzetek.leto}
              </h2>
              <p className="mb-4 text-xs text-muted">
                Stolpčni graf Chart.js • Primerjava štirih izvorov brez dvojnega štetja
              </p>
              <Bar
                data={stolpcniPodatki()}
                options={{
                  responsive: true,
                  plugins: { legend: { display: false } },
                }}
              />
            </div>
          </div>

          <div className={`${cardClass} mt-6`}>
            <h2 className="mb-3 text-[17px] font-semibold">
              Tabela deležev izvorov v letu {podatki.povzetek.leto}
            </h2>
            <div className="overflow-x-auto">
              <table className="w-full border-collapse text-left text-sm">
                <thead>
                  <tr className="border-b border-border text-muted">
                    <th className="py-2.5 pr-4 font-semibold">Izvor odpadne hrane</th>
                    <th className="py-2.5 pr-4 font-semibold">Količina (tone)</th>
                    <th className="py-2.5 font-semibold">Delež v skupni količini (%)</th>
                  </tr>
                </thead>
                <tbody>
                  {podatki.povzetek.izvori.map((izv) => (
                    <tr key={izv.koda} className="border-b border-border">
                      <td className="py-2.5 pr-4 font-medium">{izv.naziv}</td>
                      <td className="py-2.5 pr-4">
                        {izv.tone != null
                          ? izv.tone.toLocaleString('sl-SI', { maximumFractionDigits: 1 })
                          : 'Ni podatka'}
                      </td>
                      <td className="py-2.5">
                        {izv.odstotek != null ? `${izv.odstotek.toFixed(1)} %` : 'Ni podatka'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="mt-4 text-xs text-muted">
              <strong>Metodološka opomba SURS ({podatki.licenca}):</strong> {podatki.opozorilo}
            </p>
          </div>
        </>
      )}
    </>
  )
}
