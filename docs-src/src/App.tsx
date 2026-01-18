import { lazy, Suspense } from 'react'
import { HashRouter, Route, Routes, useLocation } from 'react-router-dom'

const HomePage = lazy(() => import('@/pages/home-page'))
const PrivacyPolicyPage = lazy(() => import('@/pages/privacy-policy-page'))

const AppRoutes = () => {
  const location = useLocation()

  return (
    <Suspense fallback={
      <div className="flex min-h-screen w-full items-center justify-center bg-background">
        <div className="text-center">
          <div className="inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-current border-r-transparent motion-reduce:animate-[spin_1.5s_linear_infinite]" role="status">
            <span className="sr-only">Loading...</span>
          </div>
        </div>
      </div>
    }>
      <Routes location={location} key={location.pathname}>
        <Route path="/" element={<HomePage/>}/>
        <Route path="/privacy-policy" element={<PrivacyPolicyPage/>}/>
      </Routes>
    </Suspense>
  )
}

const App = () => {
  return (
    <HashRouter>
      <div className="flex min-h-screen w-full bg-background">
        <AppRoutes />
      </div>
    </HashRouter>
  )
}

export default App
