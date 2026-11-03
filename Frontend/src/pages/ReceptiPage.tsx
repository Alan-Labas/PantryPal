import { useState, type FormEvent } from 'react'
import { AlertCircle, BookOpen, Download, ExternalLink, Search } from 'lucide-react'
import { ThemeToggle } from '../components/ThemeToggle.tsx'
import { buttonClass, cardClass, inputClass, labelClass } from '../components/ui.ts'
import { useRecepti } from '../hooks/useRecepti.ts'

const fieldClass = 'flex min-w-0 flex-col gap-1.5'

export function ReceptiPage() {
  const { recepti, porocilo, loading, scraping, error, zazeniParser } = useRecepti()
  const [stevilo, setStevilo] = useState('5')
  const [iskanje, setIskanje] = useState('')

  const onScrape = async (e: FormEvent) => {
    e.preventDefault()
    const n = Number(stevilo)
    if (n >= 1 && n <= 500) {
      await zazeniParser(n)
    }
  }

  const filtriraniRecepti = recepti.filter((r) => {
    if (!iskanje.trim()) return true
    const q = iskanje.toLowerCase()
    return (
      r.naslov?.toLowerCase().includes(q) ||
      r.kategorija?.toLowerCase().includes(q) ||
      r.sestavine?.some((s) => s.toLowerCase().includes(q))
    )
  })

  return (
    <>
      <div className="mb-6 flex items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Spletni parser receptov</h1>
          <p className="mt-1 text-[15px] text-muted">
            Samodejno razčlenjevanje receptov s portala <code>kulinarika.net</code> in shranjevanje v{' '}
            <code>data/recepti/recepti.json</code>
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

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <form onSubmit={onScrape} className={cardClass}>
          <h2 className="mb-3 flex items-center gap-2 text-[17px] font-semibold">
            <Download size={18} className="text-primary" />
            Zajem receptov s spleta
          </h2>
          <div className="flex flex-col gap-3">
            <div className={fieldClass}>
              <label htmlFor="stevilo-receptov" className={labelClass}>
                Število receptov za zajem (1–500; med zahtevami je 2 s premora)
              </label>
              <input
                id="stevilo-receptov"
                type="number"
                min="1"
                max="500"
                required
                className={inputClass}
                value={stevilo}
                onChange={(e) => setStevilo(e.target.value)}
              />
            </div>
            <button type="submit" className={buttonClass} disabled={scraping}>
              <Download size={17} />
              {scraping
                ? `Zajemam ${stevilo} receptov s Kulinarika.net…`
                : 'Zaženi spletni razčlenjevalnik (parser)'}
            </button>
          </div>
        </form>

        <div className={cardClass}>
          <h2 className="mb-3 flex items-center gap-2 text-[17px] font-semibold">
            <BookOpen size={18} className="text-primary" />
            Poročilo zadnjega zajema (porocilo.json)
          </h2>
          {porocilo.vir ? (
            <div className="space-y-1.5 text-sm">
              <p>
                <span className="text-muted">Vir:</span> <code>{porocilo.vir}</code>
              </p>
              <p>
                <span className="text-muted">Shranjeno receptov:</span>{' '}
                <strong>{porocilo.shranjeno ?? recepti.length}</strong> (zahtevano:{' '}
                {porocilo.zahtevano ?? '-'})
              </p>
              <p>
                <span className="text-muted">Prebrane strani seznama:</span>{' '}
                {porocilo.prebraneStrani ?? '-'}
              </p>
              <p>
                <span className="text-muted">Status zaključka:</span> {porocilo.razlogZakljucka ?? '-'}
              </p>
            </div>
          ) : (
            <p className="text-sm text-muted">
              Lokalna zbirka še ni bila zajeta. V levi kartici izberi število (npr. 5) in zaženi parser.
            </p>
          )}
        </div>
      </div>

      <div className="mt-8 flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
        <h2 className="text-[17px] font-semibold">
          Zajeti recepti v lokalni zbirki ({filtriraniRecepti.length})
        </h2>
        <div className="relative w-full sm:w-72">
          <Search size={16} className=" pointer-events-none absolute left-3 top-3.5 text-muted" />
          <input
            type="text"
            placeholder="Išči po naslovu ali sestavini…"
            className={`${inputClass} pl-9`}
            value={iskanje}
            onChange={(e) => setIskanje(e.target.value)}
          />
        </div>
      </div>

      {loading ? (
        <p className="mt-3 text-sm text-muted">Nalaganje receptov…</p>
      ) : filtriraniRecepti.length === 0 ? (
        <div className={`${cardClass} mt-3`}>
          <p className="text-sm text-muted">
            Ni receptov za prikaz. Zaženi spletni parser zgoraj za polnjenje zbirke.
          </p>
        </div>
      ) : (
        <div className="mt-3 grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-4">
          {filtriraniRecepti.map((r) => (
            <div className={cardClass} key={r.id}>
              <div className="flex items-start justify-between gap-2">
                <h3 className="font-semibold">{r.naslov}</h3>
                {r.kategorija && (
                  <span className="shrink-0 rounded-md bg-primary-soft px-2 py-0.5 text-xs font-medium text-primary">
                    {r.kategorija}
                  </span>
                )}
              </div>
              <p className="mt-1 text-xs text-muted">
                Porcije: {r.porcije || 'Ni podatka'} • Čas: {r.skupniCas || 'Ni podatka'}
              </p>
              <div className="mt-3">
                <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted">
                  Sestavine ({r.sestavine?.length ?? 0}):
                </p>
                <ul className="max-h-36 list-disc space-y-0.5 overflow-y-auto pl-4 text-xs text-fg">
                  {(r.sestavine ?? []).map((s, idx) => (
                    <li key={idx}>{s}</li>
                  ))}
                </ul>
              </div>
              {r.virUrl && (
                <a
                  href={r.virUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                >
                  Izvorni recept <ExternalLink size={12} />
                </a>
              )}
            </div>
          ))}
        </div>
      )}
    </>
  )
}
