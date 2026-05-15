import { getToken } from './auth'

export interface SSECallbacks {
  onToken: (content: string) => void
  onDone: (data: Record<string, unknown>) => void
  onError: (code: string, message: string) => void
}

export interface SSEStream {
  abort: () => void
  promise: Promise<void>
}

export function readSSEStream(
  url: string,
  baseUrl: string,
  callbacks: SSECallbacks,
): SSEStream {
  const abortController = new AbortController()

  const promise = doReadSSE(url, baseUrl, callbacks, abortController)

  return {
    abort: () => abortController.abort(),
    promise,
  }
}

async function doReadSSE(
  url: string,
  baseUrl: string,
  callbacks: SSECallbacks,
  abortController: AbortController,
): Promise<void> {
  const token = getToken()
  if (!token) {
    callbacks.onError('UNAUTHORIZED', '未登录')
    return
  }

  const fullUrl = url.startsWith('http') ? url : `${baseUrl}${url}`

  let response: Response
  try {
    response = await fetch(fullUrl, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
      signal: abortController.signal,
    })
  } catch (err) {
    if ((err as Error).name === 'AbortError') return
    callbacks.onError('FETCH_ERROR', err instanceof Error ? err.message : '网络请求失败')
    return
  }

  if (!response.ok) {
    callbacks.onError('HTTP_ERROR', `HTTP ${response.status}`)
    return
  }

  const reader = response.body?.getReader()
  if (!reader) {
    callbacks.onError('STREAM_ERROR', '无法读取流')
    return
  }

  const decoder = new TextDecoder()
  let buffer = ''
  let currentEvent = 'token'
  let receivedDone = false

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed) {
          currentEvent = 'token'
          continue
        }

        if (trimmed.startsWith('event:')) {
          currentEvent = trimmed.substring(6).trim()
          continue
        }

        if (trimmed.startsWith('data:')) {
          const dataStr = trimmed.substring(5).trim()
          if (!dataStr) continue

          try {
            const parsed = JSON.parse(dataStr)

            if (currentEvent === 'done') {
              receivedDone = true
              callbacks.onDone(parsed)
            } else if (currentEvent === 'error') {
              callbacks.onError(parsed.code || 'INTERNAL_ERROR', parsed.message || 'Unknown error')
            } else {
              callbacks.onToken(parsed.content || '')
            }
          } catch {
            // non-JSON data line, skip
          }
        }
      }
    }
  } catch (err) {
    if ((err as Error).name === 'AbortError') return
    if (receivedDone) return
    callbacks.onError('STREAM_ERROR', err instanceof Error ? err.message : 'Stream read error')
  } finally {
    reader.releaseLock()
  }
}
