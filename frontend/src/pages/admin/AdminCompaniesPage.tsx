import { FormEvent, useEffect, useState } from 'react';
import { adminApi } from '../../api/client';
import type { AdminCompany } from '../../api/types';

export default function AdminCompaniesPage() {
  const [companies, setCompanies] = useState<AdminCompany[]>([]);
  const [name, setName] = useState('');
  const [error, setError] = useState<string | null>(null);

  const load = () => adminApi.companies().then(setCompanies).catch((e: Error) => setError(e.message));

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
      setError((err as Error).message);
    }
  }

  async function remove(company: AdminCompany) {
    if (!confirm(`Remove "${company.name}"? It will no longer be visible to users and its pending requests will be denied.`)) {
      return;
    }
    setError(null);
    try {
      await adminApi.removeCompany(company.id);
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <section>
      <h1>Companies</h1>
      <form onSubmit={create} className="inline-form">
        <label>
          Company name
          <input required maxLength={200} value={name} onChange={(e) => setName(e.target.value)} />
        </label>
        <button type="submit" disabled={!name.trim()}>Create company</button>
      </form>
      {error && <p className="error">{error}</p>}
      {companies.length === 0 ? (
        <p>No companies yet.</p>
      ) : (
        <table>
          <thead>
            <tr><th>ID</th><th>Name</th><th>Created</th><th /></tr>
          </thead>
          <tbody>
            {companies.map((c) => (
              <tr key={c.id}>
                <td>{c.id}</td>
                <td>{c.name}</td>
                <td>{new Date(c.createdAt).toLocaleString()}</td>
                <td><button className="danger" onClick={() => remove(c)}>Remove</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
