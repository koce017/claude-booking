import { FormEvent, useEffect, useState } from 'react';
import { adminApi, getSession } from '../../api/client';
import type { Administrator } from '../../api/types';
import { errorMessage } from '../../errors';
import { formatTimestamp } from '../../format';
import { fmt, t } from '../../i18n/sr';

export default function AdministratorsPage() {
  const [admins, setAdmins] = useState<Administrator[] | null>(null);
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const me = getSession()?.email.toLowerCase();

  const load = () =>
    adminApi
      .administrators()
      .then(setAdmins)
      .catch((e) => setError(errorMessage(e)));

  useEffect(() => {
    load();
  }, []);

  async function add(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await adminApi.addAdministrator(email.trim());
      setEmail('');
      await load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function remove(a: Administrator) {
    if (!confirm(fmt(t.administrators.removeConfirm, { email: a.email }))) return;
    setError(null);
    try {
      await adminApi.removeAdministrator(a.id);
      await load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <section>
      <h1>{t.administrators.title}</h1>
      <p className="muted">{t.administrators.help}</p>
      <form onSubmit={add} className="inline-form">
        <label>
          {t.administrators.email}
          <input type="email" required maxLength={320} value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <button type="submit">{t.administrators.add}</button>
      </form>
      {error && <p className="error">{error}</p>}
      {admins && (
        <table>
          <thead>
            <tr>
              <th>{t.administrators.email}</th>
              <th>{t.administrators.since}</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {admins.map((a) => (
              <tr key={a.id}>
                <td>
                  {a.email} {a.email.toLowerCase() === me && <span className="muted">{t.administrators.you}</span>}
                </td>
                <td>{formatTimestamp(a.createdAt)}</td>
                <td className="actions">
                  {a.email.toLowerCase() !== me && (
                    <button className="danger" onClick={() => remove(a)}>
                      {t.common.remove}
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
