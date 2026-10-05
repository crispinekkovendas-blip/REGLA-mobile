import type { ReactNode } from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { reviewApplication } from '@regla/shared';
import { ApplicationReview } from '../src/components/ApplicationReview';
import { getSupabase } from '../src/lib/supabase';

jest.mock('@regla/shared', () => ({
  ...jest.requireActual('@regla/shared'),
  reviewApplication: jest.fn(() => Promise.resolve()),
}));

const mockReview = reviewApplication as jest.MockedFunction<typeof reviewApplication>;

function wrap(ui: ReactNode) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { retry: false, gcTime: Infinity } } });
  return <QueryClientProvider client={qc}>{ui}</QueryClientProvider>;
}

const props = { applicationId: 12, currentStatus: 'submitted' as const, currentNote: null, reviewerId: 'user-1' };

beforeEach(() => mockReview.mockClear());

describe('ApplicationReview', () => {
  it('approves with the "approved" status after confirming', async () => {
    const onDone = jest.fn();
    await render(wrap(<ApplicationReview {...props} onDone={onDone} />));
    await fireEvent.press(screen.getByTestId('review-approved'));
    expect(mockReview).not.toHaveBeenCalled(); // two-step: selecting does not submit
    await fireEvent.press(screen.getByTestId('review-confirm'));
    await waitFor(() => expect(mockReview).toHaveBeenCalledTimes(1));
    expect(mockReview).toHaveBeenCalledWith(getSupabase(), 12, 'approved', null, 'user-1');
    await waitFor(() => expect(onDone).toHaveBeenCalledWith('approved'));
  });

  it('marks as under review', async () => {
    await render(wrap(<ApplicationReview {...props} />));
    await fireEvent.press(screen.getByTestId('review-under_review'));
    await fireEvent.press(screen.getByTestId('review-confirm'));
    await waitFor(() => expect(mockReview).toHaveBeenCalledWith(getSupabase(), 12, 'under_review', null, 'user-1'));
  });

  it('requires a note to reject, then sends "rejected" with the note', async () => {
    await render(wrap(<ApplicationReview {...props} />));
    await fireEvent.press(screen.getByTestId('review-rejected'));
    await fireEvent.press(screen.getByTestId('review-confirm'));
    expect(mockReview).not.toHaveBeenCalled();
    expect(screen.getByText('Explique o motivo da recusa para o cliente.')).toBeTruthy();

    await fireEvent.changeText(screen.getByTestId('review-note'), '  Renda abaixo do exigido  ');
    await fireEvent.press(screen.getByTestId('review-confirm'));
    await waitFor(() =>
      expect(mockReview).toHaveBeenCalledWith(getSupabase(), 12, 'rejected', 'Renda abaixo do exigido', 'user-1'));
  });

  it('requests documents with "docs_requested"', async () => {
    await render(wrap(<ApplicationReview {...props} />));
    await fireEvent.press(screen.getByTestId('review-docs_requested'));
    await fireEvent.changeText(screen.getByTestId('review-note'), 'Envie os 3 últimos holerites');
    await fireEvent.press(screen.getByTestId('review-confirm'));
    await waitFor(() =>
      expect(mockReview).toHaveBeenCalledWith(getSupabase(), 12, 'docs_requested', 'Envie os 3 últimos holerites', 'user-1'));
  });

  it('shows a read-only state for withdrawn proposals', async () => {
    await render(wrap(<ApplicationReview {...props} currentStatus="withdrawn" />));
    expect(screen.getByText('O cliente cancelou esta proposta.')).toBeTruthy();
    expect(screen.queryByTestId('review-confirm')).toBeNull();
  });
});
