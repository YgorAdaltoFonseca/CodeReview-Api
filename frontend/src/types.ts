export type Role = 'DEVELOPER' | 'REVIEWER' | 'ADMIN'
export type ReviewStatus = 'OPEN' | 'IN_REVIEW' | 'CHANGES_REQUESTED' | 'APPROVED' | 'REJECTED' | 'CANCELLED'

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface UserReference {
  id: number
  name: string
}

export interface ReviewRequest {
  id: number
  title: string
  description: string
  repositoryUrl: string
  pullRequestUrl: string
  status: ReviewStatus
  requester: UserReference
  reviewer: UserReference | null
  createdAt: string
  updatedAt: string
}

export interface ReviewComment {
  id: number
  reviewRequestId: number
  authorId: number
  authorName: string
  content: string
  createdAt: string
}

export interface HistoryItem {
  oldStatus: ReviewStatus | null
  newStatus: ReviewStatus
  changedBy: UserReference
  changedAt: string
}

export interface AdminUser {
  id: number
  name: string
  email: string
  role: Role
  createdAt: string
}

export interface AuthUser {
  id: number
  email: string
  role: Role
}
