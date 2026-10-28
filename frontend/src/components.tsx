import { Link, Navigate, Outlet } from 'react-router'
import type { ReactNode } from 'react'
import { useAuth } from './useAuth'

export function ProtectedRoute() {
  const { token } = useAuth()
  return token ? <Outlet /> : <Navigate to="/login" replace />
}

export function AppLayout({ children }: { children: ReactNode }) {
  const { user, signOut } = useAuth()
  return <div className="app-shell">
    <header className="topbar">
      <Link className="brand" to="/">CodeReview<span> API</span></Link>
      <nav aria-label="Navegação principal">
        {user?.role === 'DEVELOPER' && <Link to="/">Solicitações</Link>}
        {user?.role === 'DEVELOPER' && <Link to="/reviews/new">Nova solicitação</Link>}
        {user?.role === 'REVIEWER' && <Link to="/">Fila de revisão</Link>}
        {user?.role === 'ADMIN' && <Link to="/admin">Administração</Link>}
      </nav>
      <div className="account"><span>{user?.email}</span><button className="button button-quiet" onClick={signOut}>Sair</button></div>
    </header>
    <main className="page-container">{children}</main>
  </div>
}

export function PageTitle({ eyebrow, title, description, action }: { eyebrow: string; title: string; description: string; action?: ReactNode }) {
  return <div className="page-title"><div><p className="eyebrow">{eyebrow}</p><h1>{title}</h1><p className="muted">{description}</p></div>{action}</div>
}

export function Feedback({ message, kind = 'error' }: { message: string; kind?: 'error' | 'success' }) {
  if (!message) return null
  return <p className={`feedback ${kind}`} role="status">{message}</p>
}

export function Loading() { return <div className="empty-state">Carregando…</div> }
