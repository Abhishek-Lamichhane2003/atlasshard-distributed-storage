import { useState } from 'react';
import { api } from '../lib/api';

export default function AuthPanel({onClose}:{onClose:(email?: string)=>void}) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(register = false) {
    setBusy(true);
    setError('');
    try {
      const result = register ? await api.register(email, password) : await api.login(email, password);
      onClose(result.email);
    } catch {
      setError(register ? 'Account creation failed. Check the email and password.' : 'Sign in failed.');
    } finally {
      setBusy(false);
    }
  }

  return <div className="modal-backdrop">
    <div className="auth-card">
      <h2>Access control plane</h2>
      <p>Sign in to upload objects, manage versions, simulate node failure, and create share links.</p>
      <input value={email} onChange={e => setEmail(e.target.value)} placeholder="Email" autoComplete="email" />
      <input value={password} onChange={e => setPassword(e.target.value)} type="password" placeholder="Password" autoComplete="current-password" />
      {error && <small className="error-text">{error}</small>}
      <button className="primary" disabled={busy || !email || password.length < 8} onClick={() => submit(false)}>Sign in</button>
      <button className="ghost" disabled={busy || !email || password.length < 8} onClick={() => submit(true)}>Create account</button>
      <button className="close" onClick={() => onClose()}>Close</button>
    </div>
  </div>;
}
