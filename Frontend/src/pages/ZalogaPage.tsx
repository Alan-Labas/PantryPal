import { useState, type FormEvent } from 'react'
import { AlertCircle, Code2, Filter, PackageOpen, RotateCcw } from 'lucide-react'
import { ThemeToggle } from '../components/ThemeToggle.tsx'
import {
  buttonClass,
  cardClass,
  inputClass,
  labelClass,
  secondaryButtonClass,
} from '../components/ui.ts'
import { useZaloga } from '../hooks/useZaloga.ts'

const fieldClass = 'flex min-w-0 flex-col gap-1.5'

export function ZalogaPage() {
  const { zivila, trgovine, postavke, filtriranoJson, filtriranoXml, loading, error, refresh } =
    useZaloga()

  const [datum, setDatum] = useState('')
  const [trgovinaId, setTrgovinaId] = useState('')
  const [ziviloId, setZiviloId] = useState('')
  const [prikaziIzvoz, setPrikaziIzvoz] = useState<'none' | 'json' | 'xml'>('none')

  const onFilter = async (e: FormEvent) => {
    e.preventDefault()
    await refresh({
      datum: datum || undefined,
      trgovinaId: trgovinaId || undefined,
      ziviloId: ziviloId || undefined,
    })
  }

  const onReset = async () => {
    setDatum('')
    setTrgovinaId('')
    setZiviloId('')
    await refresh()
  }

  return (
    <>
      <div className="mb-6 flex items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Domača zaloga</h1>
          <p className="mt-1 text-[15px] text-muted">
            Branje in povezovanje <code>zivila.xml</code>, <code>trgovine.xml</code> in{' '}
            <code>zaloga.xml</code> prek ID-jev ter samodejni izvoz v <code>filtrirano.json</code> in{' '}
            <code>filtrirano.xml</code>
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

      <form onSubmit={onFilter} className={cardClass}>
        <h2 className="mb-3 flex items-center gap-2 text-[17px] font-semibold">
          <Filter size={18} className="text-primary" />
          Filtriranje zaloge
        </h2>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
          <div className={fieldClass}>
            <label htmlFor="filter-datum" className={labelClass}>
              Rok uporabe (še uporabno na datum)
            </label>
            <input
              id="filter-datum"
              type="date"
              className={inputClass}
              value={datum}
              onChange={(e) => setDatum(e.target.value)}
            />
          </div>

          <div className={fieldClass}>
            <label htmlFor="filter-trgovina" className={labelClass}>
              Trgovina nakupa (trgovinaId)
            </label>
            <select
              id="filter-trgovina"
              className={inputClass}
              value={trgovinaId}
              onChange={(e) => setTrgovinaId(e.target.value)}
            >
              <option value="">Vse trgovine ({trgovine.length})</option>
              {trgovine.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.id} – {t.ime} ({t.kraj})
                </option>
              ))}
            </select>
          </div>

          <div className={fieldClass}>
            <label htmlFor="filter-zivilo" className={labelClass}>
              Živilo (ziviloId)
            </label>
            <select
              id="filter-zivilo"
              className={inputClass}
              value={ziviloId}
              onChange={(e) => setZiviloId(e.target.value)}
            >
              <option value="">Vsa živila ({zivila.length})</option>
              {zivila.map((z) => (
                <option key={z.id} value={z.id}>
                  {z.id} – {z.ime} ({z.kategorija})
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="mt-4 flex flex-wrap gap-2.5">
          <button type="submit" className={`${buttonClass} sm:w-auto`} disabled={loading}>
            <Filter size={17} />
            {loading ? 'Filtriram…' : 'Filtriraj in izvozi'}
          </button>
          <button type="button" className={secondaryButtonClass} onClick={onReset}>
            <RotateCcw size={16} />
            Ponastavi filtre
          </button>
          <button
            type="button"
            className={secondaryButtonClass}
            onClick={() => setPrikaziIzvoz((p) => (p === 'json' ? 'none' : 'json'))}
          >
            <Code2 size={16} />
            {prikaziIzvoz === 'json' ? 'Skrij filtrirano.json' : 'Prikaži filtrirano.json'}
          </button>
          <button
            type="button"
            className={secondaryButtonClass}
            onClick={() => setPrikaziIzvoz((p) => (p === 'xml' ? 'none' : 'xml'))}
          >
            <Code2 size={16} />
            {prikaziIzvoz === 'xml' ? 'Skrij filtrirano.xml' : 'Prikaži filtrirano.xml'}
          </button>
        </div>

        {prikaziIzvoz !== 'none' && (
          <div className="mt-4">
            <p className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-muted">
              Vsebina izvožene datoteke ({prikaziIzvoz === 'json' ? 'filtrirano.json' : 'filtrirano.xml'}):
            </p>
            <pre className="max-h-80 overflow-auto rounded-lg border border-border bg-bg p-3.5 font-mono text-xs text-fg">
              {prikaziIzvoz === 'json' ? filtriranoJson : filtriranoXml}
            </pre>
          </div>
        )}
      </form>

      <h2 className="mb-3 mt-8 flex items-center gap-2 text-[17px] font-semibold">
        <PackageOpen size={18} className="text-primary" />
        Povezani zapisi zaloge ({postavke.length})
      </h2>

      {loading ? (
        <p className="text-sm text-muted">Nalaganje XML podatkov…</p>
      ) : postavke.length === 0 ? (
        <div className={cardClass}>
          <p className="text-sm text-muted">Ni zadetkov za izbrane filtre.</p>
        </div>
      ) : (
        <div className="grid grid-cols-[repeat(auto-fill,minmax(250px,1fr))] gap-4">
          {postavke.map((entry) => (
            <div className={cardClass} key={entry.id}>
              <div className="flex items-start justify-between gap-2">
                <h3 className="font-semibold">{entry.imeZivila}</h3>
                <span className="rounded-md bg-primary-soft px-2 py-0.5 text-xs font-medium text-primary">
                  {entry.id}
                </span>
              </div>
              <p className="mt-0.5 text-xs text-muted">
                {entry.kategorija} • {entry.opisZivila}
              </p>
              <div className="mt-3 space-y-1 text-sm">
                <p>
                  <span className="text-muted">Količina:</span>{' '}
                  <strong>
                    {entry.kolicina} {entry.enota}
                  </strong>
                </p>
                <p>
                  <span className="text-muted">Trgovina:</span> {entry.imeTrgovine} ({entry.kraj}) [
                  {entry.trgovinaId}]
                </p>
                <p>
                  <span className="text-muted">Datum nakupa:</span> {entry.datumNakupa}
                </p>
                <p>
                  <span className="text-muted">Rok uporabe:</span>{' '}
                  {entry.rokUporabe ? (
                    <span className="font-medium text-fg">{entry.rokUporabe}</span>
                  ) : (
                    <span className="italic text-danger">Manjkajoč podatek (prazno polje v XML)</span>
                  )}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  )
}
