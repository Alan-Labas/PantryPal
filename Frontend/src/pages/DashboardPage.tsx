import { Link } from 'react-router-dom'
import { BarChart3, BookOpen, PackageOpen, Sparkles } from 'lucide-react'
import { ThemeToggle } from '../components/ThemeToggle.tsx'
import { cardClass } from '../components/ui.ts'

const sections = [
  {
    icon: PackageOpen,
    title: 'Domača zaloga (XML & JSON)',
    text: 'Branje treh povezanih XML datotek (zivila.xml, trgovine.xml, zaloga.xml), filtriranje in izvoz v filtrirano.json ter filtrirano.xml.',
    to: '/zaloga',
  },
  {
    icon: BookOpen,
    title: 'Spletni parser receptov',
    text: 'Samodejno razčlenjevanje receptov s portala Kulinarika.net ter shranjevanje in pregled v strukturirani JSON zbirki.',
    to: '/recepti',
  },
  {
    icon: BarChart3,
    title: 'Odprti podatki (PCAXIS & Chart.js)',
    text: 'Dinamični prenos PC-Axis datoteke 2780705S.PX s portala SURS/OPSI, filtriranje, izračun deležev in interaktivni Chart.js grafi.',
    to: '/odprti-podatki',
  },
  {
    icon: Sparkles,
    title: 'AI Asistent (Google Gemini LLM)',
    text: 'Dinamično sestavljanje prompta iz obrazca, časa v brskalniku in domače zaloge ter deserializacija JSON odgovora v Java razred AiOdgovor.',
    to: '/ai-asistent',
  },
]

export function DashboardPage() {
  const hour = new Date().getHours()
  const greeting = hour < 12 ? 'Dobro jutro' : hour < 18 ? 'Dober dan' : 'Dober večer'

  return (
    <>
      <div className="mb-6 flex items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Nadzorna plošča</h1>
          <p className="mt-1 text-[15px] text-muted">
            {greeting} — izberi sklop za demonstracijo posamezne naloge.
          </p>
        </div>
        <ThemeToggle />
      </div>

      <div className="grid grid-cols-[repeat(auto-fill,minmax(260px,1fr))] gap-4">
        {sections.map(({ icon: Icon, title, text, to }) => (
          <Link
            key={title}
            to={to}
            className={`${cardClass} block cursor-pointer text-fg no-underline transition-colors duration-200 hover:border-primary hover:no-underline`}
          >
            <div className="mb-3 flex items-center justify-between">
              <span className="inline-flex rounded-lg bg-primary-soft p-2.5 text-primary">
                <Icon size={22} />
              </span>
            </div>
            <h2 className="mb-1.5 text-[17px] font-semibold">{title}</h2>
            <p className="text-sm text-muted">{text}</p>
          </Link>
        ))}
      </div>
    </>
  )
}
