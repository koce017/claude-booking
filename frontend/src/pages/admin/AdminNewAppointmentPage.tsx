import { FormEvent, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../../api/client';
import type { AdminAppointment, AdminCompany, CustomerInput, SlotView } from '../../api/types';
import CustomerFields, { emptyCustomer } from '../../components/CustomerFields';
import SlotPicker from '../../components/SlotPicker';
import { errorMessage, fieldErrors } from '../../errors';
import { todayIso } from '../../format';
import { fmt, t } from '../../i18n/sr';

export default function AdminNewAppointmentPage() {
  const [companies, setCompanies] = useState<AdminCompany[]>([]);
  const [companyId, setCompanyId] = useState<number | null>(null);
  const [date, setDate] = useState(todayIso());
  const [slot, setSlot] = useState<SlotView | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [customer, setCustomer] = useState<CustomerInput>(emptyCustomer);
  const [note, setNote] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [created, setCreated] = useState<AdminAppointment | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    adminApi.companies().then(setCompanies).catch((e) => setError(errorMessage(e)));
  }, []);

  const company = companies.find((c) => c.id === companyId);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!company || !slot) return;
    setSaving(true);
    setError(null);
    setErrors({});
    try {
      setCreated(await adminApi.createAppointment(company.id, slot.start, customer, note));
      setCustomer(emptyCustomer);
      setNote('');
      setReloadKey((k) => k + 1);
    } catch (err) {
      setError(errorMessage(err));
      setErrors(fieldErrors(err));
      setReloadKey((k) => k + 1);
    } finally {
      setSaving(false);
    }
  }

  return (
    <section>
      <h1>{t.newAppointment.title}</h1>
      <p className="muted">{t.newAppointment.help}</p>
      {created && (
        <p className="success">
          {fmt(t.newAppointment.created, { id: created.id })}{' '}
          <Link to={`/admin/appointments?id=${created.id}`}>#{created.id}</Link>
        </p>
      )}
      <div className="inline-form">
        <label>
          {t.newAppointment.company}
          <select
            value={companyId ?? ''}
            onChange={(e) => {
              setCompanyId(e.target.value ? Number(e.target.value) : null);
              setCreated(null);
            }}
          >
            <option value="">{t.newAppointment.chooseCompany}</option>
            {companies.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
      </div>
      {company && (
        <>
          <SlotPicker
            companyId={company.id}
            date={date}
            onDateChange={setDate}
            selected={slot}
            onSelect={setSlot}
            reloadKey={reloadKey}
          />
          <form className="panel-form" onSubmit={submit}>
            {!slot && <p className="muted">{t.newAppointment.chooseSlot}</p>}
            <CustomerFields form={company.customerForm} value={customer} onChange={setCustomer} errors={errors} lenient />
            <label>
              {t.newAppointment.note}
              <textarea rows={2} maxLength={1000} value={note} onChange={(e) => setNote(e.target.value)} />
            </label>
            <div>
              <button type="submit" disabled={!slot || saving}>
                {saving ? t.common.saving : t.newAppointment.create}
              </button>
            </div>
          </form>
        </>
      )}
      {error && <p className="error">{error}</p>}
    </section>
  );
}
