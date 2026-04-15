import type { AuditEntry, ConfigEntry, DemoResponse, FeatureEval } from './types'

const prefix = import.meta.env.VITE_API_BASE ?? ''

async function parseJson<T>(res: Response): Promise<T> {
  const text = await res.text()
  if (!res.ok) {
    throw new Error(text || `${res.status} ${res.statusText}`)
  }
  if (!text) return undefined as T
  return JSON.parse(text) as T
}

export async function fetchConfigs(): Promise<ConfigEntry[]> {
  const res = await fetch(`${prefix}/admin/configs`)
  return parseJson<ConfigEntry[]>(res)
}

export async function fetchAudit(): Promise<AuditEntry[]> {
  const res = await fetch(`${prefix}/admin/audit`)
  return parseJson<AuditEntry[]>(res)
}

export async function upsertConfig(
  key: string,
  body: { value: string; rolloutPercent: number; updatedBy: string },
): Promise<ConfigEntry> {
  const res = await fetch(`${prefix}/admin/config/${encodeURIComponent(key)}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  return parseJson<ConfigEntry>(res)
}

export async function fetchDemo(userId: string): Promise<DemoResponse> {
  const q = new URLSearchParams()
  if (userId) q.set('userId', userId)
  const res = await fetch(`${prefix}/api/demo?${q.toString()}`)
  return parseJson<DemoResponse>(res)
}

export async function fetchFeatures(userId: string): Promise<FeatureEval[]> {
  const q = new URLSearchParams()
  if (userId) q.set('userId', userId)
  const res = await fetch(`${prefix}/api/features?${q.toString()}`)
  return parseJson<FeatureEval[]>(res)
}
