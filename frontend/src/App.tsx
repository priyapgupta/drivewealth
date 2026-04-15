import { useCallback, useEffect, useState } from 'react'
import {
  fetchAudit,
  fetchConfigs,
  fetchDemo,
  fetchFeatures,
  upsertConfig,
} from './api'
import type { AuditEntry, ConfigEntry, DemoResponse, FeatureEval } from './types'
import './App.css'

function formatTime(iso: string | undefined) {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString()
  } catch {
    return iso
  }
}

export default function App() {
  const [configs, setConfigs] = useState<ConfigEntry[]>([])
  const [audit, setAudit] = useState<AuditEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [key, setKey] = useState('')
  const [value, setValue] = useState('')
  const [rollout, setRollout] = useState(0)
  const [updatedBy, setUpdatedBy] = useState('dashboard')
  const [saving, setSaving] = useState(false)

  const [demoUserId, setDemoUserId] = useState('alice')
  const [demoResult, setDemoResult] = useState<DemoResponse | null>(null)
  const [featuresResult, setFeaturesResult] = useState<FeatureEval[] | null>(null)
  const [demoLoading, setDemoLoading] = useState(false)

  const loadAll = useCallback(async () => {
    setError(null)
    setLoading(true)
    try {
      const [c, a] = await Promise.all([fetchConfigs(), fetchAudit()])
      setConfigs(c)
      setAudit([...a].sort((x, y) => (y.at > x.at ? 1 : -1)))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Request failed')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadAll()
  }, [loadAll])

  async function onSave(e: React.FormEvent) {
    e.preventDefault()
    if (!key.trim()) {
      setError('Config key is required.')
      return
    }
    setSaving(true)
    setError(null)
    try {
      await upsertConfig(key.trim(), {
        value,
        rolloutPercent: Math.min(100, Math.max(0, Math.round(rollout))),
        updatedBy: updatedBy.trim() || 'dashboard',
      })
      await loadAll()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  function onEdit(row: ConfigEntry) {
    setKey(row.key)
    setValue(row.value ?? '')
    setRollout(row.rolloutPercent)
    setUpdatedBy(row.updatedBy ?? 'dashboard')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  async function onRunDemo() {
    setDemoLoading(true)
    setError(null)
    try {
      const uid = demoUserId.trim()
      const [r, f] = await Promise.all([fetchDemo(uid), fetchFeatures(uid)])
      setDemoResult(r)
      setFeaturesResult(f)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Demo request failed')
      setDemoResult(null)
      setFeaturesResult(null)
    } finally {
      setDemoLoading(false)
    }
  }

  return (
    <>
      <header>
        <h1>ConfigFlow</h1>
        <p className="subtitle">
          Feature flags and rollout controls — same data as{' '}
          <code className="inline-code">/admin</code> APIs
        </p>
      </header>

      {error && (
        <div className="alert error" role="alert">
          {error}
        </div>
      )}

      <section className="panel">
        <h2>Create or update config</h2>
        <form onSubmit={onSave}>
          <div className="form-grid cols-2">
            <div>
              <label htmlFor="cfg-key">Key</label>
              <input
                id="cfg-key"
                value={key}
                onChange={(e) => setKey(e.target.value)}
                placeholder="e.g. feature.newCheckout"
                autoComplete="off"
              />
            </div>
            <div>
              <label htmlFor="cfg-rollout">Rollout % (0–100)</label>
              <input
                id="cfg-rollout"
                type="number"
                min={0}
                max={100}
                value={rollout}
                onChange={(e) => setRollout(Number(e.target.value))}
              />
            </div>
            <div className="full-row">
              <label htmlFor="cfg-value">Value</label>
              <textarea
                id="cfg-value"
                value={value}
                onChange={(e) => setValue(e.target.value)}
                placeholder="true, JSON string, or any config value"
              />
            </div>
            <div>
              <label htmlFor="cfg-by">Updated by</label>
              <input
                id="cfg-by"
                value={updatedBy}
                onChange={(e) => setUpdatedBy(e.target.value)}
                placeholder="your name or service id"
              />
            </div>
          </div>
          <div className="toolbar" style={{ marginTop: '1rem', marginBottom: 0 }}>
            <button type="submit" className="primary" disabled={saving}>
              {saving ? 'Saving…' : 'Save'}
            </button>
            <button
              type="button"
              onClick={() => {
                setKey('')
                setValue('')
                setRollout(0)
                setUpdatedBy('dashboard')
              }}
            >
              Clear form
            </button>
          </div>
        </form>
      </section>

      <section className="panel">
        <div className="toolbar">
          <h2 style={{ margin: 0, flex: 1 }}>Configs</h2>
          <button type="button" onClick={() => void loadAll()} disabled={loading}>
            {loading ? 'Loading…' : 'Refresh'}
          </button>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Key</th>
                <th>Value</th>
                <th>Rollout</th>
                <th>Updated</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {configs.length === 0 && !loading ? (
                <tr>
                  <td colSpan={5} style={{ color: 'var(--muted)' }}>
                    No configs yet.
                  </td>
                </tr>
              ) : (
                configs.map((c) => (
                  <tr key={c.key}>
                    <td className="mono">{c.key}</td>
                    <td className="mono">{c.value}</td>
                    <td>{c.rolloutPercent}%</td>
                    <td className="mono">
                      {formatTime(c.updatedAt)}
                      <div style={{ color: 'var(--muted)', fontSize: '0.75rem' }}>
                        {c.updatedBy} · v{c.version}
                      </div>
                    </td>
                    <td>
                      <button type="button" className="linkish" onClick={() => onEdit(c)}>
                        Edit
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <h2>Feature demo (feature.newCheckout)</h2>
        <p style={{ color: 'var(--muted)', fontSize: '0.9rem', marginTop: 0 }}>
          Calls <code className="inline-code">GET /api/demo</code> — deterministic per{' '}
          <code className="inline-code">userId</code>.
        </p>
        <div className="form-grid cols-2" style={{ alignItems: 'end' }}>
          <div>
            <label htmlFor="demo-user">User ID</label>
            <input
              id="demo-user"
              value={demoUserId}
              onChange={(e) => setDemoUserId(e.target.value)}
              placeholder="alice"
            />
          </div>
          <div>
            <button
              type="button"
              className="primary"
              onClick={() => void onRunDemo()}
              disabled={demoLoading}
            >
              {demoLoading ? 'Running…' : 'Run demo'}
            </button>
          </div>
        </div>
        {demoResult && (
          <div className="demo-result">
            <div>
              <span className={demoResult.enabled ? 'badge' : 'badge off'}>
                {demoResult.enabled ? 'ENABLED' : 'disabled'}
              </span>{' '}
              <strong>{demoResult.result}</strong>
            </div>
            <div style={{ marginTop: '0.5rem', color: 'var(--muted)' }}>
              userId: {demoResult.userId ?? '(none)'} · {demoResult.feature}
            </div>
          </div>
        )}

        {featuresResult && (
          <div style={{ marginTop: '1rem' }}>
            <div style={{ color: 'var(--muted)', fontSize: '0.9rem', marginBottom: '0.5rem' }}>
              All feature flags for this user (<code className="inline-code">GET /api/features</code>)
            </div>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Feature</th>
                    <th>Status</th>
                    <th>Rollout</th>
                    <th>Value</th>
                  </tr>
                </thead>
                <tbody>
                  {featuresResult.length === 0 ? (
                    <tr>
                      <td colSpan={4} style={{ color: 'var(--muted)' }}>
                        No feature flags found.
                      </td>
                    </tr>
                  ) : (
                    featuresResult.map((f) => (
                      <tr key={f.key}>
                        <td className="mono">{f.key}</td>
                        <td>
                          <span className={f.enabled ? 'badge' : 'badge off'}>
                            {f.enabled ? 'ENABLED' : 'disabled'}
                          </span>
                        </td>
                        <td>{f.rolloutPercent}%</td>
                        <td className="mono">{f.value}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </section>

      <section className="panel">
        <h2>Audit trail</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>When</th>
                <th>Key</th>
                <th>Value</th>
                <th>Rollout</th>
                <th>By</th>
                <th>Ver</th>
              </tr>
            </thead>
            <tbody>
              {audit.length === 0 && !loading ? (
                <tr>
                  <td colSpan={6} style={{ color: 'var(--muted)' }}>
                    No audit entries.
                  </td>
                </tr>
              ) : (
                audit.map((a) => (
                  <tr key={`${a.id}-${a.at}`}>
                    <td className="mono">{formatTime(a.at)}</td>
                    <td className="mono">{a.key}</td>
                    <td className="mono">{a.value}</td>
                    <td>{a.rolloutPercent}%</td>
                    <td>{a.updatedBy}</td>
                    <td>{a.version}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      <p className="footer-links">
        API: <a href="/api/health">/api/health</a>
        {' · '}
        <a href="/h2-console" target="_blank" rel="noreferrer">
          H2 console
        </a>
      </p>
    </>
  )
}
