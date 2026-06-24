'use client';

import { FormEvent, useState } from 'react';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import { Lock, Mail, ShieldCheck } from 'lucide-react';
import { API_URL, writeSession, type StoredSession } from '@/lib/api';

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState('admin@voltstock.com');
  const [password, setPassword] = useState('');
  const [tenantSlug, setTenantSlug] = useState('volt-stock-master');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await fetch(`${API_URL}/v1/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password, tenantSlug })
      });

      const payload = await response.json();
      if (!response.ok) {
        throw new Error(payload.message ?? 'Falha no login.');
      }

      writeSession(payload as StoredSession);
      router.push('/dashboard');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha no login.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-screen">
      <section className="login-panel" aria-label="Login Volt Stock">
        <div className="login-brand">
          <Image src="/logo-volt-chat.png" alt="Volt Stock" width={188} height={188} priority />
          <div>
            <p className="eyebrow">Acesso Seguro</p>
            <h1>Volt Stock</h1>
          </div>
        </div>

        <form onSubmit={submit} className="login-form">
          <label>
            <span>E-mail</span>
            <div className="input-icon">
              <Mail size={18} aria-hidden="true" />
              <input value={email} onChange={(event) => setEmail(event.target.value)} type="email" autoComplete="email" required />
            </div>
          </label>

          <label>
            <span>Senha</span>
            <div className="input-icon">
              <Lock size={18} aria-hidden="true" />
              <input value={password} onChange={(event) => setPassword(event.target.value)} type="password" autoComplete="current-password" required />
            </div>
          </label>

          <label>
            <span>Tenant</span>
            <div className="input-icon">
              <ShieldCheck size={18} aria-hidden="true" />
              <input value={tenantSlug} onChange={(event) => setTenantSlug(event.target.value)} type="text" required />
            </div>
          </label>

          {error ? <p className="form-error">{error}</p> : null}

          <button className="primary-button" type="submit" disabled={loading}>
            <ShieldCheck size={18} aria-hidden="true" />
            <span>{loading ? 'Validando' : 'Entrar'}</span>
          </button>
        </form>
      </section>
    </main>
  );
}
