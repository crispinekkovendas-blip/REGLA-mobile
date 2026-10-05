import { fireEvent, render, screen } from '@testing-library/react-native';
import { ProfileEditor } from '@/components/ProfileEditor';
import { emptyProfileForm, validateProfileForm } from '@/lib/forms';

jest.mock('@/lib/supabase', () => ({ isConfigured: false, supabase: {}, SUPABASE_URL: '', listingPhotoUrl: () => '' }));

const VALID_CPF = '529.982.247-25';

function filled() {
  return {
    ...emptyProfileForm('ana@example.com', 'Ana Souza'),
    phone: '(11) 93221-0855',
    cpf: VALID_CPF,
    birth_date: '25/12/1990',
    employment_type: 'clt' as const,
    occupation: 'Designer',
    monthly_income: '12.000',
  };
}

describe('ProfileEditor (Meu cadastro)', () => {
  it('shows an inline error for an invalid CPF and does not save', async () => {
    const onSave = jest.fn();
    await render(<ProfileEditor initial={{ ...filled(), cpf: '' }} onSave={onSave} />);
    await fireEvent.changeText(screen.getByTestId('field-cpf'), '11111111111');
    // mask is applied while typing
    expect(screen.getByTestId('field-cpf').props.value).toBe('111.111.111-11');
    await fireEvent.press(screen.getByTestId('btn-save-profile'));
    expect(screen.getByText('CPF inválido')).toBeTruthy();
    expect(screen.getByTestId('form-error-summary')).toBeTruthy();
    expect(onSave).not.toHaveBeenCalled();
  });

  it('flags missing name, bad e-mail and short phone', async () => {
    const onSave = jest.fn();
    await render(<ProfileEditor initial={{ ...filled(), full_name: '', email: 'nope', phone: '(11) 9' }} onSave={onSave} />);
    await fireEvent.press(screen.getByTestId('btn-save-profile'));
    expect(screen.getByText('Informe seu nome completo')).toBeTruthy();
    expect(screen.getByText('E-mail inválido')).toBeTruthy();
    expect(screen.getByText('Telefone inválido')).toBeTruthy();
    expect(onSave).not.toHaveBeenCalled();
  });

  it('saves a valid cadastro as the shared ClientProfileInput shape', async () => {
    const onSave = jest.fn();
    await render(<ProfileEditor initial={filled()} onSave={onSave} />);
    await fireEvent.press(screen.getByTestId('btn-save-profile'));
    expect(onSave).toHaveBeenCalledWith({
      full_name: 'Ana Souza',
      email: 'ana@example.com',
      phone: '11932210855',
      cpf: '52998224725',
      birth_date: '1990-12-25',
      occupation: 'Designer',
      employment_type: 'clt',
      monthly_income: 12000,
      residents: 1,
      has_pets: false,
    });
  });
});

describe('validateProfileForm', () => {
  it('rejects impossible birth dates', () => {
    const r = validateProfileForm({ ...filled(), birth_date: '31/02/1990' });
    expect(r.ok).toBe(false);
    expect(r.errors.birth_date).toMatch(/Data inválida/);
  });

  it('strict mode (proposal wizard) requires CPF, income and employment', () => {
    const r = validateProfileForm({ ...filled(), cpf: '', monthly_income: '', employment_type: null }, true);
    expect(r.ok).toBe(false);
    expect(r.errors).toMatchObject({
      cpf: 'Informe seu CPF',
      monthly_income: 'Informe sua renda mensal',
      employment_type: 'Selecione sua ocupação',
    });
    expect(validateProfileForm({ ...filled(), cpf: '' }, false).ok).toBe(true);
  });
});
