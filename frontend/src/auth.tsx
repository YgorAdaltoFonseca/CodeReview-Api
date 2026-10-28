import { useMemo, useState, type ReactNode } from 'react'
import { configureApiTokenReader } from './api'
import type { AuthUser, Role } from './types'
import { AuthContext, type AuthContextValue } from './authContext'

function readUser(token: string): AuthUser {
  const payload = token.split('.')[1]
  const claims = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/'))) as { sub: string; role: Role; userId: number }
  return { id: claims.userId, email: claims.sub, role: claims.role }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null)
  const value = useMemo<AuthContextValue>(() => ({
    token,
    user: token ? readUser(token) : null,
    signIn: setToken,
    signOut: () => setToken(null),
  }), [token])
  configureApiTokenReader(() => token)
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
