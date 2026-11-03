import { FormEvent, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { adminApi, getSession } from '../../api/client';
import { errorMessage } from '../../errors';
import { t } from '../../i18n/sr';

export default function AdminLoginPage() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (getSession()) return <Navigate to="/admin/appointments" replace />;

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await adminApi.requestLink(email.trim());
      setSent(true);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <section>
      <h1>{t.login.title}</h1>
      {sent ? (
        <p className="success">
          {t.login.sent}
          <br />
          <span className="muted">{t.login.devHint}</span>
        </p>
      ) : (
        <form onSubmit={submit} className="inline-form">
          <label>
            {t.login.email}
            <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </label>
          <button type="submit">{t.login.send}</button>
        </form>
      )}
      {error && <p className="error">{error}</p>}
    </section>
  );
}
