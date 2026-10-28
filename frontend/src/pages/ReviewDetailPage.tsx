import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { api, ApiError } from '../api'
import { useAuth } from '../useAuth'
import { Feedback, Loading, PageTitle } from '../components'
import type { HistoryItem, ReviewComment, ReviewRequest } from '../types'

export function ReviewDetailPage() {
  const { id: rawId } = useParams()
  const id = Number(rawId)
  const { user } = useAuth()
  const [review, setReview] = useState<ReviewRequest | null>(null)
  const [comments, setComments] = useState<ReviewComment[]>([])
  const [history, setHistory] = useState<HistoryItem[]>([])
  const [comment, setComment] = useState('')
  const [editTitle, setEditTitle] = useState('')
  const [editDescription, setEditDescription] = useState('')
  const [decision, setDecision] = useState('APPROVED')
  const [decisionComment, setDecisionComment] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)

  const reload = useCallback(async () => {
    const result = await api.getReview(id)
    setReview(result); setEditTitle(result.title); setEditDescription(result.description)
    const [commentList, statusHistory] = await Promise.all([api.listComments(id), api.getHistory(id)])
    setComments(commentList); setHistory(statusHistory.history)
  }, [id])

  useEffect(() => {
    if (!Number.isFinite(id)) return
    let active = true
    async function loadPage() {
      try {
        const result = await api.getReview(id)
        if (!active) return
        setReview(result); setEditTitle(result.title); setEditDescription(result.description)
        const [commentList, statusHistory] = await Promise.all([api.listComments(id), api.getHistory(id)])
        if (!active) return
        setComments(commentList); setHistory(statusHistory.history)
      } catch (cause) {
        if (active) setError(cause instanceof ApiError ? cause.message : 'Não foi possível carregar a solicitação.')
      } finally {
        if (active) setLoading(false)
      }
    }
    void loadPage()
    return () => { active = false }
  }, [id])

  async function perform(action: () => Promise<unknown>, success: string) {
    setError(''); setMessage(''); setBusy(true)
    try { await action(); await reload(); setMessage(success) }
    catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Não foi possível concluir a operação.') }
    finally { setBusy(false) }
  }
  function submitComment(event: FormEvent<HTMLFormElement>) { event.preventDefault(); void perform(async () => { await api.addComment(id, comment); setComment('') }, 'Comentário adicionado.') }
  function submitDecision(event: FormEvent<HTMLFormElement>) { event.preventDefault(); void perform(() => api.decideReview(id, decision, decisionComment), 'Decisão registrada.') }
  function saveChanges(event: FormEvent<HTMLFormElement>) { event.preventDefault(); void perform(() => api.updateReview(id, { title: editTitle, description: editDescription }), 'Solicitação atualizada.') }

  if (!Number.isFinite(id)) return <Feedback message="Identificador de solicitação inválido." />
  if (loading) return <Loading />
  if (!review) return <><Link className="text-link back-link" to="/">← Voltar</Link><Feedback message={error || 'Solicitação não encontrada.'} /></>
  const isOwner = user?.role === 'DEVELOPER' && review.requester.id === user.id
  const isAssignedReviewer = user?.role === 'REVIEWER' && review.reviewer?.id === user.id

  return <>
    <Link className="text-link back-link" to="/">← Voltar às solicitações</Link>
    <PageTitle eyebrow={`SOLICITAÇÃO #${review.id}`} title={review.title} description={`Criada por ${review.requester.name} em ${new Date(review.createdAt).toLocaleString('pt-BR')}`} action={<span className={`status status-${review.status.toLowerCase()}`}>{review.status.replaceAll('_', ' ')}</span>} />
    <Feedback message={error} /><Feedback message={message} kind="success" />
    <div className="detail-grid"><section className="panel detail-main">
      <div className="section-heading"><div><p className="eyebrow">CONTEXTO</p><h2>Sobre a solicitação</h2></div></div>
      {isOwner && review.status === 'OPEN' ? <form className="form-stack compact-form" onSubmit={saveChanges}><label>Título<input required maxLength={100} value={editTitle} onChange={event => setEditTitle(event.target.value)} /></label><label>Descrição<textarea required maxLength={2000} rows={5} value={editDescription} onChange={event => setEditDescription(event.target.value)} /></label><button className="button button-secondary" disabled={busy}>Salvar alterações</button></form> : <p className="description-text">{review.description}</p>}
      <div className="link-list"><a href={review.repositoryUrl} target="_blank" rel="noreferrer">Abrir repositório ↗</a><a href={review.pullRequestUrl} target="_blank" rel="noreferrer">Abrir Pull Request ↗</a></div>
      {isOwner && ['OPEN', 'CHANGES_REQUESTED'].includes(review.status) && <button className="button button-danger" disabled={busy} onClick={() => void perform(() => api.cancelReview(id), 'Solicitação cancelada.')}>Cancelar solicitação</button>}
      {user?.role === 'REVIEWER' && review.status === 'OPEN' && <button className="button button-primary" disabled={busy} onClick={() => void perform(() => api.claimReview(id), 'Você assumiu esta revisão.')}>Assumir revisão</button>}
      <section className="subsection"><div className="section-heading"><div><p className="eyebrow">DISCUSSÃO</p><h2>Comentários</h2></div></div>
        {comments.length ? <div className="comment-list">{comments.map(item => <article className="comment" key={item.id}><div><strong>{item.authorName}</strong><time>{new Date(item.createdAt).toLocaleString('pt-BR')}</time></div><p>{item.content}</p></article>)}</div> : <p className="muted">Nenhum comentário ainda.</p>}
        {isAssignedReviewer && review.status === 'IN_REVIEW' && <form className="form-stack comment-form" onSubmit={submitComment}><label>Adicionar comentário<textarea required maxLength={5000} rows={3} value={comment} onChange={event => setComment(event.target.value)} placeholder="Escreva uma observação para o autor…" /></label><button className="button button-secondary" disabled={busy || !comment.trim()}>Publicar comentário</button></form>}
      </section>
      {isAssignedReviewer && review.status === 'IN_REVIEW' && <section className="subsection"><div className="section-heading"><div><p className="eyebrow">CONCLUSÃO</p><h2>Registrar decisão</h2></div></div><form className="form-stack" onSubmit={submitDecision}><label>Resultado<select value={decision} onChange={event => setDecision(event.target.value)}><option value="APPROVED">Aprovado</option><option value="CHANGES_REQUESTED">Solicitar alterações</option><option value="REJECTED">Rejeitado</option></select></label><label>Comentário final (opcional)<textarea maxLength={5000} rows={3} value={decisionComment} onChange={event => setDecisionComment(event.target.value)} /></label><button className="button button-primary" disabled={busy}>Registrar decisão</button></form></section>}
    </section>
    <aside className="panel detail-aside"><p className="eyebrow">ACOMPANHAMENTO</p><h2>Histórico de status</h2><ol className="timeline">{history.map((item, index) => <li key={`${item.changedAt}-${index}`}><span className="timeline-dot" /><div><strong>{item.newStatus.replaceAll('_', ' ')}</strong><p>{item.oldStatus ? `${item.oldStatus.replaceAll('_', ' ')} → ` : 'Solicitação criada · '}{item.changedBy.name}</p><time>{new Date(item.changedAt).toLocaleString('pt-BR')}</time></div></li>)}</ol><div className="meta-block"><span>Revisor responsável</span><strong>{review.reviewer?.name ?? 'Ainda sem revisor'}</strong></div><div className="meta-block"><span>Atualizada em</span><strong>{new Date(review.updatedAt).toLocaleString('pt-BR')}</strong></div></aside></div>
  </>
}
