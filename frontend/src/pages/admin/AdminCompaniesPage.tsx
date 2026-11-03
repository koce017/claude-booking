import { FormEvent, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../../api/client';
import type { AdminCompany } from '../../api/types';
import { errorMessage } from '../../errors';
import { formatTimestamp } from '../../format';
import { fmt, t } from '../../i18n/sr';

export default function AdminCompaniesPage() {
  const [companies, setCompanies] = useState<AdminCompany[] | null>(null);
  const [name, setName] = useState('');
  const [error, setError] = useState<string | null>(null);

  const load = () =>
    adminApi
      .companies()
      .then(setCompanies)
      .catch((e) => setError(errorMessage(e)));

  useEffect(() => {
    load();
  }, []);

  async function create(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await adminApi.createCompany(name.trim());
      setName('');
      await load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function remove(company: AdminCompany) {
    if (!confirm(fmt(t.companies.removeConfirm, { name: company.name }))) return;
    setError(null);
    try {
      await adminApi.removeCompany(company.id);
      await load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <section>
      <h1>{t.companies.title}</h1>
      <form onSubmit={create} className="inline-form">
        <label>
          {t.companies.name}
          <input required maxLength={200} value={name} onChange={(e) => setName(e.target.value)} />
        </label>
        <button type="submit" disabled={!name.trim()}>
          {t.companies.create}
        </button>
      </form>
      {error && <p className="error">{error}</p>}
      {!companies && !error && <p>{t.common.loading}</p>}
      {companies?.length === 0 && <p>{t.companies.empty}</p>}
      {!!companies?.length && (
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>{t.companies.name}</th>
              <th>{t.companies.created}</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {companies.map((c) => (
              <tr key={c.id}>
                <td>{c.id}</td>
                <td>
                  <Link to={`/admin/companies/${c.id}`}>{c.name}</Link>
                  {!c.fulfillmentEmail && (
                    <>
                      <br />
                      <span className="muted">{t.companies.noFulfillmentEmail}</span>
                    </>
                  )}
                </td>
                <td>{formatTimestamp(c.createdAt)}</td>
                <td className="actions">
                  <Link to={`/admin/companies/${c.id}`}>{t.companies.settings}</Link>{' '}
                  <button className="danger" onClick={() => remove(c)}>
                    {t.common.remove}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
