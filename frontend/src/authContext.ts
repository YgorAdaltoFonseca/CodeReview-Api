import { createContext } from 'react'
import type { AuthUser } from './types'

export interface AuthContextValue {
  token: string | null
  user: AuthUser | null
  signIn: (token: string) => void
  signOut: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
