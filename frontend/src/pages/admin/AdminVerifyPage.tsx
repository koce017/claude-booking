import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { adminApi, setSession } from '../../api/client';

export default function AdminVerifyPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  // Tokens are single-use: guard against StrictMode running the effect twice.
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    const token = params.get('token');
    if (!token) {
      setError('The login link is missing its token.');
      return;
    }
    adminApi
      .verify(token)
      .then((session) => {
        setSession(session);
        navigate('/admin/companies', { replace: true });
      })
      .catch((e: Error) => setError(e.message));
  }, [params, navigate]);

  return (
    <section>
      <h1>Signing in…</h1>
      {error && (
        <p className="error">
          {error} <Link to="/admin/login">Request a new link</Link>
        </p>
      )}
    </section>
  );
}
