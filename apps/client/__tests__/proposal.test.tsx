import { useState } from 'react';
import { fireEvent, render, screen } from '@testing-library/react-native';
import { ProposalForm } from '@/components/ProposalForm';
import { WizardSteps } from '@/components/WizardSteps';
import { brDateToIso, maskMoney, validateProposalForm, type ProposalFormValues } from '@/lib/forms';
import { timelineSteps } from '@/components/StatusTimeline';

jest.mock('@/lib/supabase', () => ({ isConfigured: false, supabase: {}, SUPABASE_URL: '', listingPhotoUrl: () => '' }));

const TODAY = new Date(2026, 9, 4);

const base: ProposalFormValues = {
  intent: 'rent',
  offered_price: '4.500',
  guarantee_type: 'seguro_fianca',
  move_in_date: '01/11/2026',
  message: '  Adorei o imóvel!  ',
};

describe('validateProposalForm', () => {
  it('produces the shared ApplicationInput for a valid rent proposal', () => {
    const r = validateProposalForm(42, base, TODAY);
    expect(r.ok).toBe(true);
    expect(r.data).toEqual({
      listing_id: 42,
      intent: 'rent',
      offered_price: 4500,
      guarantee_type: 'seguro_fianca',
      move_in_date: '2026-11-01',
      message: 'Adorei o imóvel!',
    });
  });

  it('requires a price and a guarantee for rentals', () => {
    const r = validateProposalForm(42, { ...base, offered_price: '', guarantee_type: null }, TODAY);
    expect(r.ok).toBe(false);
    expect(r.errors.offered_price).toBe('Informe um valor');
    expect(r.errors.guarantee_type).toBe('Escolha uma garantia');
  });

  it('rejects invalid or past move-in dates', () => {
    expect(validateProposalForm(42, { ...base, move_in_date: '32/13/2026' }, TODAY).errors.move_in_date).toMatch(/Data inválida/);
    expect(validateProposalForm(42, { ...base, move_in_date: '01/01/2020' }, TODAY).errors.move_in_date).toMatch(/hoje ou depois/);
  });

  it('drops the guarantee for purchase offers and accepts an empty date', () => {
    const r = validateProposalForm(7, { ...base, intent: 'buy', offered_price: '1.800.000', guarantee_type: null, move_in_date: '', message: '' }, TODAY);
    expect(r.ok).toBe(true);
    expect(r.data).toMatchObject({ intent: 'buy', offered_price: 1_800_000, guarantee_type: null, move_in_date: null, message: null });
  });

  it('helpers mask money and convert BR dates', () => {
    expect(maskMoney('0012345')).toBe('12.345');
    expect(brDateToIso('25/12/1990')).toBe('1990-12-25');
    expect(brDateToIso('')).toBeNull();
    expect(brDateToIso('29/02/2025')).toBeUndefined();
  });
});

function Harness({ income }: { income: number | null }) {
  const [v, setV] = useState<ProposalFormValues>(base);
  return <ProposalForm values={v} onChange={setV} errors={{}} askingPrice={4500} currency="BRL" monthlyIncome={income} />;
}

describe('ProposalForm', () => {
  it('shows the affordability badge from monthly income and updates as the offer changes', async () => {
    await render(<Harness income={20000} />);
    expect(screen.getByTestId('affordability-ok')).toBeTruthy(); // 4.500 / 20.000 = 22.5%
    await fireEvent.changeText(screen.getByTestId('field-offered_price'), '7000');
    expect(screen.getByTestId('field-offered_price').props.value).toBe('7.000');
    expect(screen.getByTestId('affordability-tight')).toBeTruthy(); // 35%
    await fireEvent.changeText(screen.getByTestId('field-offered_price'), '9000');
    expect(screen.getByTestId('affordability-over')).toBeTruthy(); // 45%
  });

  it('shows "unknown" when income is missing and hides guarantees for purchase', async () => {
    await render(<Harness income={null} />);
    expect(screen.getByTestId('affordability-unknown')).toBeTruthy();
    expect(screen.getByText('Seguro fiança')).toBeTruthy();
    await fireEvent.press(screen.getByText('Comprar'));
    expect(screen.queryByText('Seguro fiança')).toBeNull();
    expect(screen.getByText('Valor da oferta')).toBeTruthy();
  });
});

describe('wizard + status timeline', () => {
  it('labels the current wizard step', async () => {
    await render(<WizardSteps current={2} />);
    expect(screen.getByLabelText('Etapa 3 de 4')).toBeTruthy();
    expect(screen.getByText('3. Documentos')).toBeTruthy();
  });

  it('derives timeline states from application status', () => {
    expect(timelineSteps('submitted').map((s) => s.state)).toEqual(['done', 'todo', 'todo']);
    expect(timelineSteps('under_review').map((s) => s.state)).toEqual(['done', 'current', 'todo']);
    expect(timelineSteps('docs_requested').map((s) => s.label)).toEqual(['Enviada', 'Em análise', 'Documentos pendentes', 'Decisão']);
    expect(timelineSteps('approved').at(-1)).toMatchObject({ label: 'Aprovada', state: 'done' });
    expect(timelineSteps('rejected').at(-1)).toMatchObject({ label: 'Recusada', state: 'failed' });
  });
});
