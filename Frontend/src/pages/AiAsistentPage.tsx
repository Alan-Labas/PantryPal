import { useEffect, useState, type FormEvent } from 'react'
import { AlertCircle, Clock, Code2, Eye, Sparkles, Utensils } from 'lucide-react'
import { ThemeToggle } from '../components/ThemeToggle.tsx'
import {
  buttonClass,
  cardClass,
  inputClass,
  labelClass,
  secondaryButtonClass,
} from '../components/ui.ts'
import { useLlm } from '../hooks/useLlm.ts'

const fieldClass = 'flex min-w-0 flex-col gap-1.5'

export function AiAsistentPage() {
  const { predogled, rezultat, loading, error, osveziPredogled, generiraj } = useLlm()

  const [vrstaObroka, setVrstaObroka] = useState('Kosilo')
  const [najvecjiCasMin, setNajvecjiCasMin] = useState(30)
  const [steviloPorcij, setSteviloPorcij] = useState(2)
  const [prehranskeOmejitve, setPrehranskeOmejitve] = useState('Brez posebnih omejitev')
  const [poudarek, setPoudarek] = useState(
    'Prednostno porabi živila, ki jim najprej poteče rok uporabe',
  )
  const [dodatnaZelja, setDodatnaZelja] = useState('')

  const [prikaziPrompt, setPrikaziPrompt] = useState(false)
  const [prikaziJson, setPrikaziJson] = useState(false)

  const zgradiZahtevo = () => ({
    vrstaObroka,
    najvecjiCasMin,
    steviloPorcij,
    prehranskeOmejitve,
    poudarek,
    dodatnaZelja,
    casBrskalnika: new Date().toLocaleString('sl-SI'),
    casovniPas: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Ljubljana',
    jezikBrskalnika: navigator.language || 'sl-SI',
  })

  useEffect(() => {
    void osveziPredogled({
      vrstaObroka,
      najvecjiCasMin,
      steviloPorcij,
      prehranskeOmejitve,
      poudarek,
      dodatnaZelja,
      casBrskalnika: new Date().toLocaleString('sl-SI'),
      casovniPas: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Ljubljana',
      jezikBrskalnika: navigator.language || 'sl-SI',
    })
  }, [
    vrstaObroka,
    najvecjiCasMin,
    steviloPorcij,
    prehranskeOmejitve,
    poudarek,
    dodatnaZelja,
    osveziPredogled,
  ])

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    await generiraj(zgradiZahtevo())
  }

  const trenutniKontekst = zgradiZahtevo()

  return (
    <>
      <div className="mb-6 flex items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">AI Asistent za recepte</h1>
          <p className="mt-1 text-[15px] text-muted">
            Dinamično sestavljanje prompta iz obrazca, časa v brskalniku in domače zaloge ter
            deserializacija JSON odgovora v Java razred <code>AiOdgovor</code>
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

      <form onSubmit={onSubmit} className={cardClass}>
        <h2 className="mb-3 flex items-center gap-2 text-[17px] font-semibold">
          <Sparkles size={18} className="text-primary" />
          Obrazec vhodnih podatkov za dinamični prompt
        </h2>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <div className={fieldClass}>
            <label htmlFor="llm-obrok" className={labelClass}>
              Vrsta obroka (dropdown)
            </label>
            <select
              id="llm-obrok"
              className={inputClass}
              value={vrstaObroka}
              onChange={(e) => setVrstaObroka(e.target.value)}
            >
              <option value="Zajtrk">Zajtrk</option>
              <option value="Kosilo">Kosilo</option>
              <option value="Večerja">Večerja</option>
              <option value="Hitra malica / prigrizek">Hitra malica / prigrizek</option>
            </select>
          </div>

          <div className={fieldClass}>
            <label htmlFor="llm-cas" className={labelClass}>
              Največji čas priprave (min)
            </label>
            <input
              id="llm-cas"
              type="number"
              min="5"
              max="240"
              required
              className={inputClass}
              value={najvecjiCasMin}
              onChange={(e) => setNajvecjiCasMin(Number(e.target.value))}
            />
          </div>

          <div className={fieldClass}>
            <label htmlFor="llm-porcije" className={labelClass}>
              Število oseb / porcij
            </label>
            <input
              id="llm-porcije"
              type="number"
              min="1"
              max="12"
              required
              className={inputClass}
              value={steviloPorcij}
              onChange={(e) => setSteviloPorcij(Number(e.target.value))}
            />
          </div>

          <div className={fieldClass}>
            <label htmlFor="llm-omejitve" className={labelClass}>
              Prehranske omejitve (dropdown)
            </label>
            <select
              id="llm-omejitve"
              className={inputClass}
              value={prehranskeOmejitve}
              onChange={(e) => setPrehranskeOmejitve(e.target.value)}
            >
              <option value="Brez posebnih omejitev">Brez posebnih omejitev</option>
              <option value="Vegetarijansko">Vegetarijansko</option>
              <option value="Brez laktoze">Brez laktoze</option>
              <option value="Visokobeljakovinsko">Visokobeljakovinsko</option>
              <option value="Brez glutena">Brez glutena</option>
            </select>
          </div>

          <div className={fieldClass}>
            <label htmlFor="llm-poudarek" className={labelClass}>
              Glavni poudarek (dropdown)
            </label>
            <select
              id="llm-poudarek"
              className={inputClass}
              value={poudarek}
              onChange={(e) => setPoudarek(e.target.value)}
            >
              <option value="Prednostno porabi živila, ki jim najprej poteče rok uporabe">
                Prednostno porabi živila pred potekom roka
              </option>
              <option value="Čim hitrejša priprava z minimalno posode">
                Čim hitrejša priprava z minimalno posode
              </option>
              <option value="Topla domača jed iz osnovnih sestavin">
                Topla domača jed iz osnovnih sestavin
              </option>
            </select>
          </div>

          <div className={fieldClass}>
            <label htmlFor="llm-zelja" className={labelClass}>
              Dodatna želja uporabnika (input polje)
            </label>
            <input
              id="llm-zelja"
              type="text"
              placeholder="npr. brez pečice, uporabi jajca…"
              className={inputClass}
              value={dodatnaZelja}
              onChange={(e) => setDodatnaZelja(e.target.value)}
            />
          </div>
        </div>

        <div className="mt-4 flex flex-wrap items-center gap-2 rounded-lg border border-border bg-bg px-3.5 py-2.5 text-xs text-muted">
          <Clock size={15} className="shrink-0 text-primary" />
          <span>
            <strong>Spreminjajoči se kontekst brskalnika:</strong> Čas:{' '}
            <code>{trenutniKontekst.casBrskalnika}</code> • Časovni pas:{' '}
            <code>{trenutniKontekst.casovniPas}</code> • Jezik:{' '}
            <code>{trenutniKontekst.jezikBrskalnika}</code> • Model:{' '}
            <code>{predogled?.model ?? 'gemini-3.8-flash'}</code>
          </span>
        </div>

        <div className="mt-4 flex flex-wrap gap-2.5">
          <button type="submit" className={`${buttonClass} sm:w-auto`} disabled={loading}>
            <Sparkles size={17} />
            {loading
              ? 'Google Gemini generira strukturiran JSON…'
              : 'Pridobi AI predloge receptov (Google Gemini)'}
          </button>
          <button
            type="button"
            className={secondaryButtonClass}
            onClick={() => setPrikaziPrompt((v) => !v)}
          >
            <Eye size={16} />
            {prikaziPrompt
              ? 'Skrij dinamično sestavljen prompt'
              : 'Prikaži dinamično sestavljen prompt'}
          </button>
        </div>

        {prikaziPrompt && predogled?.prompt && (
          <div className="mt-4">
            <p className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-muted">
              Dinamično sestavljen prompt (uporabnikov vnos + čas v brskalniku + zaloga iz XML):
            </p>
            <pre className="max-h-80 overflow-auto whitespace-pre-wrap rounded-lg border border-border bg-bg p-3.5 font-mono text-xs text-fg">
              {predogled.prompt}
            </pre>
          </div>
        )}
      </form>

      {rezultat && (
        <>
          <div className="mt-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
            <div className={cardClass}>
              <p className="text-xs font-semibold uppercase text-muted">Model LLM</p>
              <p className="mt-1 text-lg font-bold text-primary">{rezultat.model}</p>
            </div>
            <div className={cardClass}>
              <p className="text-xs font-semibold uppercase text-muted">Prejeti recepti</p>
              <p className="mt-1 text-lg font-bold text-primary">
                {rezultat.odgovor.recepti.length}
              </p>
            </div>
            <div className={cardClass}>
              <p className="text-xs font-semibold uppercase text-muted">Povprečni čas priprave</p>
              <p className="mt-1 text-lg font-bold text-success">
                {rezultat.povprecniCasPripraveMin.toFixed(1)} min
              </p>
            </div>
            <div className={cardClass}>
              <p className="text-xs font-semibold uppercase text-muted">
                Uporabljene postavke zaloge
              </p>
              <p className="mt-1 text-lg font-bold text-fg">
                {rezultat.steviloUporabljenihZivilIzZaloge}
              </p>
            </div>
          </div>

          <div className={`${cardClass} mt-6`}>
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 className="text-[17px] font-semibold">
                  Povzetek konteksta in prednostna živila pred rokom
                </h2>
                <p className="mt-1 text-sm text-muted">{rezultat.odgovor.povzetekKonteksta}</p>
              </div>
              <button
                type="button"
                className={secondaryButtonClass}
                onClick={() => setPrikaziJson((v) => !v)}
              >
                <Code2 size={16} />
                {prikaziJson
                  ? 'Skrij deserializiran JSON (AiOdgovor)'
                  : 'Prikaži deserializiran JSON (AiOdgovor)'}
              </button>
            </div>

            <div className="mt-3 flex flex-wrap gap-2">
              {(rezultat.odgovor.prednostnaZivilaPredRokom ?? []).map((z, idx) => (
                <span
                  key={idx}
                  className="rounded-full bg-primary-soft px-3 py-1 text-xs font-semibold text-primary"
                >
                  {z}
                </span>
              ))}
            </div>

            {rezultat.odgovor.nasvetZaZmanjsanjeOdpadkov && (
              <div className="mt-4 rounded-lg bg-success-soft px-3.5 py-2.5 text-sm text-success">
                <strong>Nasvet za zmanjšanje odpadkov:</strong>{' '}
                {rezultat.odgovor.nasvetZaZmanjsanjeOdpadkov}
              </div>
            )}

            {prikaziJson && (
              <div className="mt-4">
                <p className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-muted">
                  Deserializiran objekt Java razreda <code>com.example.PantryPal.vao.AiOdgovor</code>:
                </p>
                <pre className="max-h-80 overflow-auto rounded-lg border border-border bg-bg p-3.5 font-mono text-xs text-fg">
                  {JSON.stringify(rezultat.odgovor, null, 2)}
                </pre>
              </div>
            )}
          </div>

          <h2 className="mb-3 mt-8 flex items-center gap-2 text-[17px] font-semibold">
            <Utensils size={18} className="text-primary" />
            Generirani recepti ({rezultat.odgovor.recepti.length})
          </h2>

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
            {rezultat.odgovor.recepti.map((r, idx) => (
              <div className={cardClass} key={idx}>
                <div className="flex items-start justify-between gap-2">
                  <h3 className="font-semibold">
                    {idx + 1}. {r.naslov}
                  </h3>
                  <span className="shrink-0 rounded-md bg-primary-soft px-2 py-0.5 text-xs font-semibold text-primary">
                    {r.casPripraveMin} min
                  </span>
                </div>
                <p className="mt-1 text-xs text-muted">
                  {r.vrstaObroka} • {r.steviloPorcij} porcij
                </p>
                <p className="mt-2.5 text-sm text-muted">{r.utemeljitevIzbire}</p>

                <div className="mt-3">
                  <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted">
                    Uporabljena živila iz zaloge:
                  </p>
                  <div className="flex flex-wrap gap-1.5">
                    {(r.uporabljenaZivilaIzZaloge ?? []).map((z, i) => (
                      <span
                        key={i}
                        className="rounded-md bg-success-soft px-2 py-0.5 text-xs font-medium text-success"
                      >
                        {z}
                      </span>
                    ))}
                  </div>
                </div>

                <div className="mt-3">
                  <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted">
                    Manjkajoče / dodatne sestavine:
                  </p>
                  <div className="flex flex-wrap gap-1.5">
                    {(r.manjkajoceSestavine ?? []).length === 0 ? (
                      <span className="text-xs text-muted">Vse je v domači zalogi</span>
                    ) : (
                      r.manjkajoceSestavine.map((m, i) => (
                        <span
                          key={i}
                          className="rounded-md border border-border bg-bg px-2 py-0.5 text-xs text-muted"
                        >
                          {m}
                        </span>
                      ))
                    )}
                  </div>
                </div>

                <div className="mt-3">
                  <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted">
                    Postopek priprave:
                  </p>
                  <ol className="list-decimal space-y-1 pl-4 text-xs text-fg">
                    {(r.korakiPriprave ?? []).map((k, i) => (
                      <li key={i}>{k.replace(/^\d+\.\s*/, '')}</li>
                    ))}
                  </ol>
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </>
  )
}
