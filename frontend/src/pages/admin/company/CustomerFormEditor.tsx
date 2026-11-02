import { useState } from 'react';
import type { AdminCompany, CompanyUpdate, CustomerForm, FieldRequirement } from '../../../api/types';
import { t } from '../../../i18n/sr';
import { toUpdate } from './companyUpdate';

interface Props {
  company: AdminCompany;
  onSave: (update: CompanyUpdate) => Promise<boolean>;
  errors: Record<string, string>;
}

const REQUIREMENTS: FieldRequirement[] = ['REQUIRED', 'OPTIONAL', 'HIDDEN'];

export default function CustomerFormEditor({ company, onSave, errors }: Props) {
  const [form, setForm] = useState<CustomerForm>(company.customerForm);
  const [saving, setSaving] = useState(false);

  const requirement = (key: 'phone' | 'email' | 'address', label: string) => (
    <label>
      {label}
      <select value={form[key]} onChange={(e) => setForm({ ...form, [key]: e.target.value as FieldRequirement })}>
        {REQUIREMENTS.map((r) => (
          <option key={r} value={r}>
            {t.requirement[r]}
          </option>
        ))}
      </select>
    </label>
  );

  const updateQuestion = (i: number, change: Partial<CustomerForm['questions'][number]>) =>
    setForm({ ...form, questions: form.questions.map((q, j) => (j === i ? { ...q, ...change } : q)) });

  async function save() {
    setSaving(true);
    await onSave({ ...toUpdate(company), customerForm: form });
    setSaving(false);
  }

  return (
    <div className="settings-form">
      <p className="muted">{t.customerFormEdit.help}</p>
      <label>
        {t.booking.name}
        <select disabled value="REQUIRED">
          <option value="REQUIRED">{t.requirement.REQUIRED}</option>
        </select>
      </label>
      {requirement('phone', t.booking.phone)}
      {requirement('email', t.booking.email)}
      {requirement('address', t.booking.address)}

      <h3>{t.customerFormEdit.questions}</h3>
      {form.questions.length === 0 && <p className="muted">{t.customerFormEdit.noQuestions}</p>}
      {form.questions.map((q, i) => (
        <div key={i} className="question-row">
          <label>
            {t.customerFormEdit.questionLabel}
            <input
              required
              maxLength={200}
              value={q.label}
              onChange={(e) => updateQuestion(i, { label: e.target.value })}
            />
            {errors[`customerForm.questions[${i}].label`] && (
              <span className="error">{errors[`customerForm.questions[${i}].label`]}</span>
            )}
          </label>
          <label className="checkbox">
            <input type="checkbox" checked={q.required} onChange={(e) => updateQuestion(i, { required: e.target.checked })} />
            {t.customerFormEdit.required}
          </label>
          <button
            type="button"
            className="danger small"
            onClick={() => setForm({ ...form, questions: form.questions.filter((_, j) => j !== i) })}
          >
            {t.common.remove}
          </button>
        </div>
      ))}
      <p>
        <button
          type="button"
          className="secondary"
          onClick={() => setForm({ ...form, questions: [...form.questions, { key: '', label: '', required: false }] })}
        >
          {t.customerFormEdit.addQuestion}
        </button>
      </p>
      <div>
        <button onClick={save} disabled={saving || form.questions.some((q) => !q.label.trim())}>
          {saving ? t.common.saving : t.common.save}
        </button>
      </div>
    </div>
  );
}
