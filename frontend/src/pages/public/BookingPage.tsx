import { FormEvent, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, publicApi } from '../../api/client';
import type { BookingResult, Company, CustomerInput, SlotView } from '../../api/types';
import CustomerFields, { emptyCustomer } from '../../components/CustomerFields';
import SlotPicker from '../../components/SlotPicker';
import { errorMessage, fieldErrors } from '../../errors';
import { formatDate, todayIso } from '../../format';
import { fmt, t } from '../../i18n/sr';

export default function BookingPage() {
  const companyId = Number(useParams().companyId);
  const [company, setCompany] = useState<Company | null>(null);
  const [date, setDate] = useState(todayIso());
  const [selected, setSelected] = useState<SlotView | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [customer, setCustomer] = useState<CustomerInput>(emptyCustomer);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [booked, setBooked] = useState<{ result: BookingResult; slot: SlotView; date: string } | null>(null);

  useEffect(() => {
    publicApi.company(companyId).then(setCompany).catch((e) => setError(errorMessage(e)));
  }, [companyId]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!selected) return;
    setSubmitting(true);
    setError(null);
    setErrors({});
    try {
      const result = await publicApi.book(companyId, selected.start, customer);
      setBooked({ result, slot: selected, date });
      setCustomer(emptyCustomer);
    } catch (err) {
      const slotGone =
        err instanceof ApiError && (err.status === 409 || err.code === 'SLOT_IN_PAST' || err.code === 'NOT_A_WORKING_SLOT');
      setError(err instanceof ApiError && err.status === 409 ? t.booking.slotTaken : errorMessage(err));
      setErrors(fieldErrors(err));
      if (slotGone) setReloadKey((k) => k + 1);
    } finally {
      setSubmitting(false);
    }
  }

  if (booked) {
    return (
      <section>
        <h1>{t.booking.doneTitle}</h1>
        <p className="success">
          {fmt(t.booking.doneText, {
            company: company?.name ?? '',
            date: formatDate(booked.date),
            start: booked.slot.localStart,
            end: booked.slot.localEnd,
            id: booked.result.id,
          })}
        </p>
        <p>{t.booking.doneStatus}</p>
        <button
          onClick={() => {
            setBooked(null);
            setReloadKey((k) => k + 1);
          }}
        >
          {t.booking.bookAnother}
        </button>{' '}
        <Link to="/">{t.booking.backToCompanies}</Link>
      </section>
    );
  }

  return (
    <section>
      <p>
        <Link to="/">{t.booking.allCompanies}</Link>
      </p>
      <h1>{company?.name ?? t.common.loading}</h1>
      {company?.description && <p>{company.description}</p>}
      {company && (company.address || company.phone) && (
        <p className="muted">{[company.address, company.phone].filter(Boolean).join(' · ')}</p>
      )}

      {company && (
        <SlotPicker
          companyId={companyId}
          date={date}
          onDateChange={setDate}
          selected={selected}
          onSelect={(slot) => {
            setSelected(slot);
            setError(null);
          }}
          reloadKey={reloadKey}
        />
      )}
      {error && <p className="error">{error}</p>}

      {company && selected && (
        <form className="panel-form" onSubmit={submit}>
          <h2>
            {fmt(t.booking.formTitle, { date: formatDate(date), start: selected.localStart, end: selected.localEnd })}
          </h2>
          {selected.pendingRequests && <p className="muted">{t.booking.otherPending}</p>}
          <CustomerFields form={company.customerForm} value={customer} onChange={setCustomer} errors={errors} />
          <div>
            <button type="submit" disabled={submitting}>
              {submitting ? t.booking.submitting : t.booking.submit}
            </button>{' '}
            <button type="button" className="secondary" onClick={() => setSelected(null)}>
              {t.common.cancel}
            </button>
          </div>
        </form>
      )}
    </section>
  );
}
