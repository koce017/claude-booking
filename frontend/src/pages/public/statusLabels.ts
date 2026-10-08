import type { SlotView } from '../../api/types';

export function slotLabel(slot: SlotView): string {
  switch (slot.status) {
    case 'AVAILABLE':
      return 'Available';
    case 'PENDING':
      return 'Available – requests pending';
    case 'NON_WORKING':
      return 'Non-working';
    case 'UNAVAILABLE':
      if (slot.reason === 'PAST') return 'Unavailable (past)';
      if (slot.reason === 'OUTSIDE_BOOKING_WINDOW') return 'Unavailable (outside booking window)';
      return 'Unavailable (booked)';
  }
}
