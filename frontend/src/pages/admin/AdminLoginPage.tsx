import { FormEvent, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { adminApi, getSession } from '../../api/client';

export default function AdminLoginPage() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (getSession()) return <Navigate to="/admin/companies" replace />;

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await adminApi.requestLink(email.trim());
      setSent(true);
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <section>
      <h1>Admin login</h1>
      {sent ? (
        <p className="success">
          If this email belongs to an administrator, a login link has been sent. Open it to sign in.
          <br />
          <span className="muted">(Local development: the link is printed in the backend log.)</span>
        </p>
      ) : (
        <form onSubmit={submit} className="inline-form">
          <label>
            Email
            <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </label>
          <button type="submit">Send login link</button>
        </form>
      )}
      {error && <p className="error">{error}</p>}
    </section>
  );
}
