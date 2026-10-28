import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { api, ApiError } from '../api'
import { useAuth } from '../useAuth'
import { Feedback } from '../components'

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const isRegister = mode === 'register'
  const navigate = useNavigate()
  const { signIn } = useAuth()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      if (isRegister) await api.register({ name, email, password })
      const result = await api.login({ email, password })
      signIn(result.token)
      navigate('/', { replace: true })
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Não foi possível conectar à API.')
    } finally { setLoading(false) }
  }

  return <main className="auth-page">
    <section className="auth-card">
      <Link className="brand" to="/">CodeReview<span> API</span></Link>
      <p className="eyebrow">REVISÃO DE CÓDIGO, COM FOCO</p>
      <h1>{isRegister ? 'Crie sua conta' : 'Boas-vindas de volta'}</h1>
      <p className="muted">{isRegister ? 'Cadastre-se para enviar suas solicitações de revisão.' : 'Entre para acompanhar suas solicitações de revisão.'}</p>
      <form className="form-stack" onSubmit={submit}>
        {isRegister && <label>Nome<input required maxLength={100} value={name} onChange={event => setName(event.target.value)} autoComplete="name" /></label>}
        <label>E-mail<input required type="email" value={email} onChange={event => setEmail(event.target.value)} autoComplete="email" /></label>
        <label>Senha<input required type="password" minLength={8} maxLength={72} value={password} onChange={event => setPassword(event.target.value)} autoComplete={isRegister ? 'new-password' : 'current-password'} /></label>
        <Feedback message={error} />
        <button className="button button-primary full-width" disabled={loading}>{loading ? 'Aguarde…' : isRegister ? 'Criar conta' : 'Entrar'}</button>
      </form>
      <p className="auth-switch">{isRegister ? 'Já tem uma conta?' : 'Ainda não tem conta?'} <Link to={isRegister ? '/login' : '/register'}>{isRegister ? 'Entrar' : 'Cadastre-se'}</Link></p>
    </section>
  </main>
}
