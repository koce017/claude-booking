import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { publicApi } from '../../api/client';
import type { Company } from '../../api/types';

export default function CompanyListPage() {
  const [companies, setCompanies] = useState<Company[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    publicApi.companies().then(setCompanies).catch((e: Error) => setError(e.message));
  }, []);

  return (
    <section>
      <h1>Choose a company</h1>
      {error && <p className="error">{error}</p>}
      {!companies && !error && <p>Loading…</p>}
      {companies?.length === 0 && <p>No companies are available yet.</p>}
      <ul className="company-list">
        {companies?.map((c) => (
          <li key={c.id}>
            <Link to={`/companies/${c.id}`}>{c.name}</Link>
            {c.description && <span className="muted"> — {c.description}</span>}
          </li>
        ))}
      </ul>
    </section>
  );
}
