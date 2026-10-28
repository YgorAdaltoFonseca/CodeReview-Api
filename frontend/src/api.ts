import type { AdminUser, HistoryItem, PageResponse, ReviewComment, ReviewRequest, ReviewStatus, Role } from './types'

const API_URL = import.meta.env.VITE_API_URL ?? '/api'

export class ApiError extends Error {
  public status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
    this.name = 'ApiError'
  }
}

let getToken: () => string | null = () => null
export function configureApiTokenReader(reader: () => string | null) {
  getToken = reader
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken()
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })

  if (!response.ok) {
    const error = await response.json().catch(() => null) as { message?: string } | null
    throw new ApiError(error?.message ?? 'Não foi possível concluir a solicitação.', response.status)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

const json = (body: unknown): RequestInit => ({ method: 'POST', body: JSON.stringify(body) })

interface UserRoleResponse {
  id: number
  name: string
  email: string
  role: Role
}

export const api = {
  register: (body: { name: string; email: string; password: string }) => request('/auth/register', json(body)),
  login: (body: { email: string; password: string }) => request<{ token: string; tokenType: string; expiresIn: number }>('/auth/login', json(body)),
  listReviews: (page: number, status?: ReviewStatus) => request<PageResponse<ReviewRequest>>(`/reviews?page=${page}&size=10${status ? `&status=${status}` : ''}`),
  getReview: (id: number) => request<ReviewRequest>(`/reviews/${id}`),
  createReview: (body: { title: string; description: string; repositoryUrl: string; pullRequestUrl: string }) => request<ReviewRequest>('/reviews', json(body)),
  updateReview: (id: number, body: { title: string; description: string }) => request<ReviewRequest>(`/reviews/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  cancelReview: (id: number) => request<ReviewRequest>(`/reviews/${id}/cancel`, { method: 'PATCH' }),
  resubmitReview: (id: number) => request<ReviewRequest>(`/reviews/${id}/resubmit`, { method: 'PATCH' }),
  claimReview: (id: number) => request<ReviewRequest>(`/reviews/${id}/claim`, { method: 'PATCH' }),
  listComments: (id: number) => request<ReviewComment[]>(`/reviews/${id}/comments`),
  addComment: (id: number, content: string) => request<ReviewComment>(`/reviews/${id}/comments`, json({ content })),
  getHistory: (id: number) => request<{ reviewRequestId: number; history: HistoryItem[] }>(`/reviews/${id}/history`),
  decideReview: (id: number, decision: string, comment: string) => request<ReviewRequest>(`/reviews/${id}/decision`, json({ decision, comment: comment || undefined })),
  listUsers: (page: number) => request<PageResponse<AdminUser>>(`/admin/users?page=${page}&size=20`),
  changeRole: (id: number, role: Role) => request<UserRoleResponse>(`/admin/users/${id}/role`, { method: 'PATCH', body: JSON.stringify({ role }) }),
}
