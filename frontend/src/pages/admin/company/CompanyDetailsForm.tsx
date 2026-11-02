import { FormEvent, useState } from 'react';
import type { AdminCompany, CompanyUpdate } from '../../../api/types';
import { fmt, t } from '../../../i18n/sr';
import { toUpdate } from './companyUpdate';

interface Props {
  company: AdminCompany;
  onSave: (update: CompanyUpdate) => Promise<boolean>;
  errors: Record<string, string>;
}

type TextKey = 'name' | 'description' | 'logoUrl' | 'address' | 'phone' | 'email' | 'website' | 'fulfillmentEmail' | 'timezone';
type NumberKey = 'appointmentDurationMinutes' | 'slotIntervalMinutes' | 'maxConcurrentAppointments' | 'bookingWindowMonths';

const TIME_ZONES: string[] = (() => {
  try {
    return (Intl as unknown as { supportedValuesOf(key: string): string[] }).supportedValuesOf('timeZone');
  } catch {
    return [];
  }
})();

export default function CompanyDetailsForm({ company, onSave, errors }: Props) {
  const [form, setForm] = useState<CompanyUpdate>(() => toUpdate(company));
  const [saving, setSaving] = useState(false);
  const eff = company.effective;

  const text = (key: TextKey, label: string, props: Record<string, unknown> = {}) => (
    <label>
      {label}
      <input
        value={form[key] ?? ''}
        onChange={(e) => setForm({ ...form, [key]: e.target.value })}
        {...props}
      />
      {errors[key] && <span className="error">{errors[key]}</span>}
    </label>
  );

  const number = (key: NumberKey, label: string, placeholder: number | string, min: number, max: number) => (
    <label>
      {label}
      <input
        type="number"
        min={min}
        max={max}
        placeholder={fmt(t.common.defaultValue, { value: placeholder })}
        value={form[key] ?? ''}
        onChange={(e) => setForm({ ...form, [key]: e.target.value === '' ? null : Number(e.target.value) })}
      />
      {errors[key] && <span className="error">{errors[key]}</span>}
    </label>
  );

  async function submit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    await onSave({ ...form, customerForm: company.customerForm });
    setSaving(false);
  }

  return (
    <form className="settings-form" onSubmit={submit}>
      {text('name', t.companies.name, { required: true, maxLength: 200 })}

      <h3>{t.companyEdit.publicInfo}</h3>
      <label>
        {t.companyEdit.description}
        <textarea
          rows={3}
          maxLength={5000}
          value={form.description ?? ''}
          onChange={(e) => setForm({ ...form, description: e.target.value })}
        />
      </label>
      {text('address', t.companyEdit.address, { maxLength: 500 })}
      {text('phone', t.companyEdit.phone, { maxLength: 50 })}
      {text('email', t.companyEdit.email, { type: 'email', maxLength: 320 })}
      {text('website', t.companyEdit.website, { maxLength: 1000 })}
      {text('logoUrl', t.companyEdit.logoUrl, { maxLength: 1000 })}

      <h3>{t.companyEdit.internal}</h3>
      {text('fulfillmentEmail', t.companyEdit.fulfillmentEmail, { type: 'email', maxLength: 320 })}
      <p className="muted">{t.companyEdit.fulfillmentEmailHelp}</p>

      <h3>{t.companyEdit.booking}</h3>
      {text('timezone', t.companyEdit.timezone, {
        list: 'time-zones',
        placeholder: fmt(t.common.defaultValue, { value: company.timezone ? 'Europe/Belgrade' : eff.timezone }),
      })}
      <datalist id="time-zones">
        {TIME_ZONES.map((z) => (
          <option key={z} value={z} />
        ))}
      </datalist>
      {number('appointmentDurationMinutes', t.companyEdit.duration, 60, 5, 1440)}
      {number('slotIntervalMinutes', t.companyEdit.interval, eff.appointmentDurationMinutes, 5, 1440)}
      <p className="muted">{t.companyEdit.intervalHelp}</p>
      {number('maxConcurrentAppointments', t.companyEdit.capacity, 1, 1, 1000)}
      {number('bookingWindowMonths', t.companyEdit.window, 12, 1, 60)}

      <div>
        <button type="submit" disabled={saving}>
          {saving ? t.common.saving : t.common.save}
        </button>
      </div>
    </form>
  );
}
