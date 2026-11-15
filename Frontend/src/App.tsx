import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ThemeProvider } from './theme/ThemeContext.tsx'
import { AppLayout } from './components/AppLayout.tsx'
import { DashboardPage } from './pages/DashboardPage.tsx'
import { ZalogaPage } from './pages/ZalogaPage.tsx'
import { ReceptiPage } from './pages/ReceptiPage.tsx'
import { OdprtiPodatkiPage } from './pages/OdprtiPodatkiPage.tsx'
import { AiAsistentPage } from './pages/AiAsistentPage.tsx'
import { GrpcPage } from './pages/GrpcPage.tsx'

function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/zaloga" element={<ZalogaPage />} />
            <Route path="/recepti" element={<ReceptiPage />} />
            <Route path="/odprti-podatki" element={<OdprtiPodatkiPage />} />
            <Route path="/ai-asistent" element={<AiAsistentPage />} />
            <Route path="/grpc" element={<GrpcPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
  )
}

export default App
