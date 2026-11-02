import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { Availability, Company } from '../api/types';
import BookingPage from '../pages/public/BookingPage';

const company: Company = {
  id: 1,
  name: 'Servis',
  description: null,
  logoUrl: null,
  address: null,
  phone: null,
  email: null,
  website: null,
  timezone: 'Europe/Belgrade',
  customerForm: {
    phone: 'HIDDEN',
    email: 'OPTIONAL',
    address: 'REQUIRED',
    questions: [{ key: 'model', label: 'Model uređaja', required: true }],
  },
};

function availability(date: string): Availability {
  return {
    companyId: 1,
    timezone: 'Europe/Belgrade',
    appointmentDurationMinutes: 60,
    slotIntervalMinutes: 60,
    bookingWindow: { firstDate: '2026-01-01', lastDate: '2027-12-31' },
    days: [
      {
        date,
        working: true,
        slots: [
          slot(date, '10:00', '11:00', 'AVAILABLE'),
          slot(date, '11:00', '12:00', 'UNAVAILABLE', 'FULLY_BOOKED'),
        ],
      },
    ],
  };
}

function slot(date: string, from: string, to: string, status: 'AVAILABLE' | 'UNAVAILABLE', reason: 'FULLY_BOOKED' | null = null) {
  return {
    start: `${date}T${from}:00+01:00`,
    end: `${date}T${to}:00+01:00`,
    localStart: from,
    localEnd: to,
    status,
    pendingRequests: false,
    bookable: status === 'AVAILABLE',
    reason,
  };
}

const json = (body: unknown, status = 200) =>
  Promise.resolve(new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }));

let bookingResponse: () => Promise<Response>;
const fetchMock = vi.fn((url: string, init?: RequestInit) => {
  if (url === '/api/companies/1') return json(company);
  if (url.startsWith('/api/companies/1/availability')) {
    return json(availability(new URL(url, 'http://x').searchParams.get('date')!));
  }
  if (url === '/api/companies/1/appointments' && init?.method === 'POST') return bookingResponse();
  return json({}, 404);
});

function renderPage() {
  render(
    <MemoryRouter initialEntries={['/companies/1']}>
      <Routes>
        <Route path="/companies/:companyId" element={<BookingPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('BookingPage', () => {
  beforeEach(() => {
    fetchMock.mockClear();
    vi.stubGlobal('fetch', fetchMock);
  });
  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it('asks for the fields the company configured and submits them', async () => {
    bookingResponse = () => json({ id: 42, companyId: 1, start: '', end: '', status: 'PENDING' }, 201);
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: /10:00–11:00/ }));
    expect(screen.getByRole('button', { name: /11:00–12:00/ })).toHaveProperty('disabled', true);

    expect(screen.queryByLabelText(/Telefon/)).toBeNull();
    expect(screen.getByLabelText(/Adresa/)).toHaveProperty('required', true);
    expect(screen.getByLabelText(/Email/)).toHaveProperty('required', false);

    fireEvent.change(screen.getByLabelText(/Ime i prezime/), { target: { value: ' Marko ' } });
    fireEvent.change(screen.getByLabelText(/Adresa/), { target: { value: 'Bulevar 5' } });
    fireEvent.change(screen.getByLabelText(/Model uređaja/), { target: { value: 'Bosch' } });
    fireEvent.click(screen.getByRole('button', { name: 'Pošalji zahtev' }));

    expect(await screen.findByRole('heading', { name: 'Zahtev je poslat' })).toBeTruthy();
    expect(screen.getByText(/broj zahteva: 42/)).toBeTruthy();
    const [, init] = fetchMock.mock.calls.find(([, i]) => i?.method === 'POST')!;
    expect(JSON.parse(init!.body as string).customer).toEqual({
      name: 'Marko',
      address: 'Bulevar 5',
      answers: { model: 'Bosch' },
    });
  });

  it('reloads the slots when the chosen slot was taken meanwhile', async () => {
    bookingResponse = () => json({ code: 'SLOT_UNAVAILABLE', detail: 'gone' }, 409);
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: /10:00–11:00/ }));
    fireEvent.change(screen.getByLabelText(/Ime i prezime/), { target: { value: 'Marko' } });
    fireEvent.change(screen.getByLabelText(/Adresa/), { target: { value: 'Bulevar 5' } });
    fireEvent.change(screen.getByLabelText(/Model uređaja/), { target: { value: 'Bosch' } });
    const availabilityCalls = () => fetchMock.mock.calls.filter(([u]) => u.includes('/availability')).length;
    const before = availabilityCalls();
    fireEvent.click(screen.getByRole('button', { name: 'Pošalji zahtev' }));

    expect(await screen.findByText(/upravo zauzet/)).toBeTruthy();
    await waitFor(() => expect(availabilityCalls()).toBeGreaterThan(before));
  });
});
