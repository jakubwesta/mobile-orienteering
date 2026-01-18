import { HashRouter as Router, Route, Routes, useLocation } from 'react-router-dom'

import HomePage from '@/pages/home-page'
import PrivacyPolicyPage from '@/pages/privacy-policy-page'

const AppRoutes = () => {
  const location = useLocation()

  return (
    <Routes location={location} key={location.pathname}>
      <Route path="/" element={<HomePage/>}/>
      <Route path="/privacy-policy" element={<PrivacyPolicyPage/>}/>
    </Routes>
  )
}

const App = () => {
  return (
    <Router>
      <div className="flex min-h-screen w-full bg-background">
        <AppRoutes />
      </div>
    </Router>
  )
}

export default App
