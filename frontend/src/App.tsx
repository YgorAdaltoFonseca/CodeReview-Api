import { Navigate, Route, Routes } from 'react-router'
import { AppLayout, ProtectedRoute } from './components'
import { useAuth } from './useAuth'
import { AdminPage } from './pages/AdminPage'
import { AuthPage } from './pages/AuthPage'
import { CreateReviewPage } from './pages/CreateReviewPage'
import { ReviewDetailPage } from './pages/ReviewDetailPage'
import { ReviewsPage } from './pages/ReviewsPage'

function AdminRoute() {
  const { user } = useAuth()
  return user?.role === 'ADMIN' ? <AdminPage /> : <Navigate to="/" replace />
}

function DeveloperRoute() {
  const { user } = useAuth()
  return user?.role === 'DEVELOPER' ? <CreateReviewPage /> : <Navigate to="/" replace />
}

function HomeRoute() {
  const { user } = useAuth()
  return user?.role === 'ADMIN' ? <AdminPage /> : <ReviewsPage />
}

function PrivateApp() {
  return <AppLayout><Routes>
    <Route path="/" element={<HomeRoute />} />
    <Route path="/reviews/new" element={<DeveloperRoute />} />
    <Route path="/reviews/:id" element={<ReviewDetailPage />} />
    <Route path="/admin" element={<AdminRoute />} />
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes></AppLayout>
}

export default function App() {
  return <Routes>
    <Route path="/login" element={<AuthPage mode="login" />} />
    <Route path="/register" element={<AuthPage mode="register" />} />
    <Route element={<ProtectedRoute />}><Route path="/*" element={<PrivateApp />} /></Route>
  </Routes>
}
