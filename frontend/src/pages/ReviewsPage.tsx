import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, ApiError } from '../api'
import { PageTitle, Feedback, Loading } from '../components'
import { useAuth } from '../useAuth'
import type { PageResponse, ReviewRequest, ReviewStatus } from '../types'

const statuses: ReviewStatus[] = ['OPEN', 'IN_REVIEW', 'CHANGES_REQUESTED', 'APPROVED', 'REJECTED', 'CANCELLED']

export function ReviewsPage() {
  const { user } = useAuth()
  const [data, setData] = useState<PageResponse<ReviewRequest> | null>(null)
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(0)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.listReviews(page, statusFilter as ReviewStatus || undefined)
      .then(result => { setData(result); setError('') })
      .catch((cause: unknown) => setError(cause instanceof ApiError ? cause.message : 'Falha ao carregar solicitações.'))
      .finally(() => setLoading(false))
  }, [page, statusFilter])

  return <>
    <PageTitle eyebrow={user?.role === 'REVIEWER' ? 'ESPAÇO DO REVISOR' : 'VISÃO GERAL'} title={user?.role === 'REVIEWER' ? 'Fila de revisão' : 'Suas solicitações'} description={user?.role === 'REVIEWER' ? 'Solicitações abertas e revisões atribuídas a você.' : 'Acompanhe o andamento das suas revisões de código.'} action={user?.role === 'DEVELOPER' ? <Link className="button button-primary" to="/reviews/new">+ Nova solicitação</Link> : undefined} />
    <section className="panel">
      <div className="toolbar"><div><h2>Solicitações</h2><p className="muted">{data?.totalElements ?? 0} no total</p></div><label className="filter-label">Status<select value={statusFilter} onChange={event => { setLoading(true); setPage(0); setStatusFilter(event.target.value) }}><option value="">Todos</option>{statuses.map(value => <option key={value}>{value}</option>)}</select></label></div>
      <Feedback message={error} />
      {loading ? <Loading /> : data?.content.length ? <>
        <div className="table-wrap"><table><thead><tr><th>Solicitação</th><th>Status</th><th>Autor</th><th>Revisor</th><th>Criada em</th><th /></tr></thead><tbody>
          {data.content.map(review => <tr key={review.id}><td><Link className="table-link" to={`/reviews/${review.id}`}>{review.title}</Link><span className="table-subtitle">#{review.id}</span></td><td><span className={`status status-${review.status.toLowerCase()}`}>{review.status.replaceAll('_', ' ')}</span></td><td>{review.requester.name}</td><td>{review.reviewer?.name ?? <span className="muted">—</span>}</td><td>{new Date(review.createdAt).toLocaleDateString('pt-BR')}</td><td><Link className="text-link" to={`/reviews/${review.id}`}>Detalhes →</Link></td></tr>)}
        </tbody></table></div>
        <div className="pagination"><span className="muted">Página {data.page + 1} de {Math.max(data.totalPages, 1)}</span><div><button className="button button-secondary" disabled={page === 0} onClick={() => { setLoading(true); setPage(value => value - 1) }}>Anterior</button><button className="button button-secondary" disabled={page + 1 >= data.totalPages} onClick={() => { setLoading(true); setPage(value => value + 1) }}>Próxima</button></div></div>
      </> : <div className="empty-state"><strong>Nenhuma solicitação encontrada</strong><span>Quando houver itens para este filtro, eles aparecerão aqui.</span></div>}
    </section>
  </>
}
