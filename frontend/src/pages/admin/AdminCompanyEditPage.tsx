import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { adminApi } from '../../api/client';
import type { AdminCompany, CompanyUpdate } from '../../api/types';
import { errorMessage, fieldErrors } from '../../errors';
import { t } from '../../i18n/sr';
import CompanyDetailsForm from './company/CompanyDetailsForm';
import CustomerFormEditor from './company/CustomerFormEditor';
import OverridesEditor from './company/OverridesEditor';
import WeeklyScheduleEditor from './company/WeeklyScheduleEditor';

type Section = keyof typeof t.companyEdit.sections;
const SECTIONS: Section[] = ['details', 'weekly', 'overrides', 'customerForm'];

export default function AdminCompanyEditPage() {
  const companyId = Number(useParams().companyId);
  const [company, setCompany] = useState<AdminCompany | null>(null);
  const [section, setSection] = useState<Section>('details');
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    adminApi.company(companyId).then(setCompany).catch((e) => setError(errorMessage(e)));
  }, [companyId]);

  async function save(update: CompanyUpdate): Promise<boolean> {
    setError(null);
    setErrors({});
    setMessage(null);
    try {
      setCompany(await adminApi.updateCompany(companyId, update));
      setMessage(t.common.saved);
      return true;
    } catch (e) {
      setError(errorMessage(e));
      setErrors(fieldErrors(e));
      return false;
    }
  }

  if (!company) return <p>{error ?? t.common.loading}</p>;

  return (
    <section>
      <p>
        <Link to="/admin/companies">← {t.companies.title}</Link>
      </p>
      <h1>{company.name}</h1>
      {!company.active && <p className="error">{t.companyEdit.removed}</p>}
      <div className="tabs" role="tablist">
        {SECTIONS.map((s) => (
          <button
            key={s}
            role="tab"
            aria-selected={section === s}
            className={section === s ? '' : 'secondary'}
            onClick={() => {
              setSection(s);
              setMessage(null);
              setError(null);
            }}
          >
            {t.companyEdit.sections[s]}
          </button>
        ))}
      </div>
      {section === 'details' && <CompanyDetailsForm key={company.updatedAt} company={company} onSave={save} errors={errors} />}
      {section === 'weekly' && <WeeklyScheduleEditor companyId={companyId} />}
      {section === 'overrides' && <OverridesEditor companyId={companyId} />}
      {section === 'customerForm' && (
        <CustomerFormEditor key={company.updatedAt} company={company} onSave={save} errors={errors} />
      )}
      {message && <p className="success">{message}</p>}
      {error && <p className="error">{error}</p>}
    </section>
  );
}
