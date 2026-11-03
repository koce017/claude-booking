import { describe, expect, it } from 'vitest';
import { ApiError } from '../api/client';
import { errorMessage, fieldErrors } from '../errors';
import { fmt, slotLabel, t } from '../i18n/sr';
import type { SlotView } from '../api/types';

describe('texts', () => {
  it('fills placeholders and leaves unknown ones', () => {
    expect(fmt('Termin #{id} za {name} {x}', { id: 5, name: 'Ana' })).toBe('Termin #5 za Ana {x}');
  });

  it('translates backend error codes and field messages', () => {
    expect(errorMessage(new ApiError(409, 'SLOT_UNAVAILABLE', 'The requested slot is no longer available'))).toBe(
      t.errors.codes.SLOT_UNAVAILABLE,
    );
    expect(errorMessage(new ApiError(500, 'SOMETHING_NEW', 'x'))).toBe(t.errors.generic);
    expect(errorMessage(new TypeError('Failed to fetch'))).toBe(t.errors.network);
    expect(
      fieldErrors(
        new ApiError(400, 'VALIDATION_FAILED', 'x', {
          'customer.email': 'is required',
          'customer.phone': 'something unexpected',
        }),
      ),
    ).toEqual({ 'customer.email': 'Obavezno polje', 'customer.phone': t.errors.invalidValue });
  });

  it('labels slots by status and reason', () => {
    const slot = { status: 'UNAVAILABLE', reason: 'PAST' } as SlotView;
    expect(slotLabel(slot)).toBe(t.slot.PAST);
    expect(slotLabel({ ...slot, reason: null })).toBe(t.slot.FULLY_BOOKED);
    expect(slotLabel({ ...slot, status: 'AVAILABLE' })).toBe(t.slot.AVAILABLE);
  });
});
