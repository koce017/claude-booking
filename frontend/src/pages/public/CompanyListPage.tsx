import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { publicApi } from '../../api/client';
import type { Company } from '../../api/types';
import { errorMessage } from '../../errors';
import { t } from '../../i18n/sr';

export default function CompanyListPage() {
  const [companies, setCompanies] = useState<Company[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    publicApi.companies().then(setCompanies).catch((e) => setError(errorMessage(e)));
  }, []);

  return (
    <section>
      <h1>{t.companyList.title}</h1>
      {error && <p className="error">{error}</p>}
      {!companies && !error && <p>{t.common.loading}</p>}
      {companies?.length === 0 && <p>{t.companyList.empty}</p>}
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
