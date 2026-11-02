import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { adminApi, setSession } from '../../api/client';
import { errorMessage } from '../../errors';
import { t } from '../../i18n/sr';

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
      setError(t.login.missingToken);
      return;
    }
    adminApi
      .verify(token)
      .then((session) => {
        setSession(session);
        navigate('/admin/appointments', { replace: true });
      })
      .catch((e) => setError(errorMessage(e)));
  }, [params, navigate]);

  return (
    <section>
      <h1>{t.login.signingIn}</h1>
      {error && (
        <p className="error">
          {error} <Link to="/admin/login">{t.login.newLink}</Link>
        </p>
      )}
    </section>
  );
}
