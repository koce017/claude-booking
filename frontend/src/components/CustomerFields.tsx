import type { CustomerForm, CustomerInput, FieldRequirement } from '../api/types';
import { t } from '../i18n/sr';

export const emptyCustomer: CustomerInput = { name: '', phone: '', email: '', address: '', answers: {} };

interface Props {
  form: CustomerForm;
  value: CustomerInput;
  onChange: (value: CustomerInput) => void;
  errors: Record<string, string>;
  /** Administrators entering an appointment: every shown field is optional except the name. */
  lenient?: boolean;
}

/** The customer details a company asks for, following its customer form settings. */
export default function CustomerFields({ form, value, onChange, errors, lenient = false }: Props) {
  const required = (r: FieldRequirement) => r === 'REQUIRED' && !lenient;
  const label = (text: string, isRequired: boolean) => (
    <>
      {text} {!isRequired && <span className="muted">{t.booking.optional}</span>}
    </>
  );
  const error = (key: string) => errors[key] && <span className="error">{errors[key]}</span>;

  return (
    <>
      <label>
        {label(t.booking.name, true)}
        <input
          required
          maxLength={200}
          autoComplete="name"
          value={value.name}
          onChange={(e) => onChange({ ...value, name: e.target.value })}
        />
        {error('customer.name')}
      </label>
      {form.phone !== 'HIDDEN' && (
        <label>
          {label(t.booking.phone, required(form.phone))}
          <input
            required={required(form.phone)}
            type="tel"
            maxLength={50}
            pattern="[0-9+()\-./ ]+"
            autoComplete="tel"
            value={value.phone}
            onChange={(e) => onChange({ ...value, phone: e.target.value })}
          />
          {error('customer.phone')}
        </label>
      )}
      {form.email !== 'HIDDEN' && (
        <label>
          {label(t.booking.email, required(form.email))}
          <input
            required={required(form.email)}
            type="email"
            maxLength={320}
            autoComplete="email"
            value={value.email}
            onChange={(e) => onChange({ ...value, email: e.target.value })}
          />
          {error('customer.email')}
        </label>
      )}
      {form.address !== 'HIDDEN' && (
        <label>
          {label(t.booking.address, required(form.address))}
          <input
            required={required(form.address)}
            maxLength={500}
            autoComplete="street-address"
            value={value.address}
            onChange={(e) => onChange({ ...value, address: e.target.value })}
          />
          {error('customer.address')}
        </label>
      )}
      {form.questions.map((q) => (
        <label key={q.key}>
          {label(q.label, q.required && !lenient)}
          <textarea
            required={q.required && !lenient}
            maxLength={1000}
            rows={2}
            value={value.answers[q.key] ?? ''}
            onChange={(e) => onChange({ ...value, answers: { ...value.answers, [q.key]: e.target.value } })}
          />
          {error(`customer.answers.${q.key}`)}
        </label>
      ))}
    </>
  );
}
