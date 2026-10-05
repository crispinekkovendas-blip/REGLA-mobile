import { fireEvent, render, screen } from '@testing-library/react-native';
import type { ShowingWithListing } from '@regla/shared';
import { ShowingItem, showingActions } from '../src/components/rows';

const showing: ShowingWithListing = {
  id: 9, listing_id: 42, inquiry_id: null, type: 'private',
  starts_at: new Date(2026, 9, 5, 14, 0).toISOString(), duration_minutes: 45, status: 'scheduled',
  visitor_name: 'Ana Souza', visitor_email: 'ana@example.com', visitor_phone: '(11) 98888-7777',
  notes: null, created_by: null, created_at: '2026-10-01T00:00:00Z',
  listings: { id: 42, title: 'Apto Pinheiros', neighborhood: 'Pinheiros', city: 'São Paulo' },
};

describe('ShowingItem', () => {
  it('renders time window, visitor and listing ref', async () => {
    await render(<ShowingItem showing={showing} />);
    expect(screen.getByText('14:00')).toBeTruthy();
    expect(screen.getByText('14:45')).toBeTruthy();
    expect(screen.getByText('Ana Souza')).toBeTruthy();
    expect(screen.getByText('RG-0042 · Apto Pinheiros')).toBeTruthy();
    expect(screen.getByText('Agendada')).toBeTruthy();
  });

  it('fires status changes (confirmar / cancelar)', async () => {
    const onStatus = jest.fn();
    await render(<ShowingItem showing={showing} onStatus={onStatus} />);
    await fireEvent.press(screen.getByTestId('showing-9-confirmed'));
    await fireEvent.press(screen.getByTestId('showing-9-cancelled'));
    expect(onStatus.mock.calls).toEqual([['confirmed'], ['cancelled']]);
  });

  it('offers the right actions per status', () => {
    expect(showingActions('scheduled').map((a) => a.status)).toEqual(['confirmed', 'attended', 'no_show', 'cancelled']);
    expect(showingActions('confirmed').map((a) => a.status)).toEqual(['attended', 'no_show', 'cancelled']);
    expect(showingActions('attended')).toEqual([]);
  });
});
