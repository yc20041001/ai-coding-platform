export type StatusType =
  | 'ACTIVE' | 'ARCHIVED' | 'COMPLETED' | 'FAILED' | 'CANCELED'
  | 'RUNNING' | 'PENDING' | 'PROCESSING' | 'STREAMING'
  | 'DISABLED' | 'ENABLED' | 'PUBLISHED' | 'DRAFT'
  | string

export function statusTagType(status: StatusType): 'success' | 'warning' | 'danger' | 'info' | '' {
  switch (status) {
    case 'ACTIVE':
    case 'COMPLETED':
    case 'SUCCESS':
    case 'ENABLED':
    case 'PUBLISHED':
      return 'success'
    case 'RUNNING':
    case 'PENDING':
    case 'PROCESSING':
    case 'STREAMING':
      return 'warning'
    case 'FAILED':
    case 'DISABLED':
    case 'CANCELED':
      return 'danger'
    case 'ARCHIVED':
    case 'DRAFT':
      return 'info'
    default:
      return ''
  }
}
