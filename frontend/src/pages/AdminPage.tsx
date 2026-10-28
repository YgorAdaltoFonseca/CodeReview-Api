import { useEffect, useState } from 'react'
import { api, ApiError } from '../api'
import { Feedback, Loading, PageTitle } from '../components'
import type { AdminUser, PageResponse, Role } from '../types'

export function AdminPage() {
  const [data, setData] = useState<PageResponse<AdminUser> | null>(null)
  const [page, setPage] = useState(0)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  async function loadUsers(currentPage: number) {
    setLoading(true)
    try { setData(await api.listUsers(currentPage)); setError('') }
    catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Não foi possível listar os usuários.') }
    finally { setLoading(false) }
  }
  useEffect(() => {
    api.listUsers(page)
      .then(result => { setData(result); setError('') })
      .catch((cause: unknown) => setError(cause instanceof ApiError ? cause.message : 'Não foi possível listar os usuários.'))
      .finally(() => setLoading(false))
  }, [page])
  async function changeRole(user: AdminUser, role: Role) {
    try { await api.changeRole(user.id, role); await loadUsers(page) }
    catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Não foi possível alterar o papel.') }
  }

  return <><PageTitle eyebrow="ADMINISTRAÇÃO" title="Gerenciar usuários" description="Consulte usuários e atribua os papéis necessários para a revisão." />
    <section className="panel"><div className="toolbar"><div><h2>Usuários</h2><p className="muted">Papéis são controlados pela API e não podem ser definidos no cadastro público.</p></div></div><Feedback message={error} />
      {loading ? <Loading /> : data?.content.length ? <><div className="table-wrap"><table><thead><tr><th>Usuário</th><th>E-mail</th><th>Cadastro</th><th>Papel</th></tr></thead><tbody>{data.content.map(user => <tr key={user.id}><td>{user.name}</td><td>{user.email}</td><td>{new Date(user.createdAt).toLocaleDateString('pt-BR')}</td><td><select aria-label={`Papel de ${user.name}`} value={user.role} onChange={event => void changeRole(user, event.target.value as Role)}><option value="DEVELOPER">DEVELOPER</option><option value="REVIEWER">REVIEWER</option><option value="ADMIN">ADMIN</option></select></td></tr>)}</tbody></table></div><div className="pagination"><span className="muted">Página {data.page + 1} de {Math.max(1, data.totalPages)}</span><div><button className="button button-secondary" disabled={page === 0} onClick={() => { setLoading(true); setPage(value => value - 1) }}>Anterior</button><button className="button button-secondary" disabled={page + 1 >= data.totalPages} onClick={() => { setLoading(true); setPage(value => value + 1) }}>Próxima</button></div></div></> : <div className="empty-state">Nenhum usuário encontrado.</div>}
    </section>
  </>
}
