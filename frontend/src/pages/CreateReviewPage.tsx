import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { api, ApiError } from '../api'
import { Feedback, PageTitle } from '../components'

export function CreateReviewPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ title: '', description: '', repositoryUrl: '', pullRequestUrl: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  function update(field: keyof typeof form, value: string) { setForm(current => ({ ...current, [field]: value })) }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setLoading(true); setError('')
    try { const review = await api.createReview(form); navigate(`/reviews/${review.id}`) }
    catch (cause) { setError(cause instanceof ApiError ? cause.message : 'Não foi possível criar a solicitação.') }
    finally { setLoading(false) }
  }

  return <>
    <PageTitle eyebrow="NOVA SOLICITAÇÃO" title="Peça uma revisão" description="Compartilhe o pull request e explique o que deve ser analisado." />
    <section className="panel form-panel"><form className="form-stack" onSubmit={submit}>
      <label>Título<input required maxLength={100} value={form.title} onChange={event => update('title', event.target.value)} placeholder="Ex.: Revisar autenticação JWT" /></label>
      <label>Descrição<textarea required maxLength={2000} rows={5} value={form.description} onChange={event => update('description', event.target.value)} placeholder="Contexto, áreas de atenção e dúvidas específicas…" /></label>
      <label>URL do repositório<input required type="url" pattern="https://.*" value={form.repositoryUrl} onChange={event => update('repositoryUrl', event.target.value)} placeholder="https://github.com/usuario/projeto" /></label>
      <label>URL do Pull Request<input required type="url" pattern="https://.*" value={form.pullRequestUrl} onChange={event => update('pullRequestUrl', event.target.value)} placeholder="https://github.com/usuario/projeto/pull/12" /></label>
      <Feedback message={error} /><div className="form-actions"><Link className="button button-secondary" to="/">Cancelar</Link><button className="button button-primary" disabled={loading}>{loading ? 'Enviando…' : 'Criar solicitação'}</button></div>
    </form></section>
  </>
}
