export interface ConfigEntry {
  key: string
  value: string
  rolloutPercent: number
  updatedBy: string
  updatedAt: string
  version: number
}

export interface AuditEntry {
  id: number
  key: string
  updatedBy: string
  rolloutPercent: number
  value: string
  at: string
  version: number
}

export interface DemoResponse {
  userId: string | null
  feature: string
  enabled: boolean
  result: string
}

export interface FeatureEval {
  key: string
  enabled: boolean
  rolloutPercent: number
  value: string
}
