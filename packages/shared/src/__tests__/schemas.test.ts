import { describe, expect, it } from 'vitest';
import { applicationSchema, clientProfileSchema, inquirySchema, visitSchema } from '../schemas';

const profile = {
  full_name: 'Ana Cliente',
  email: 'ana@cliente.test',
  phone: '(11) 93221-0855',
  cpf: '529.982.247-25',
  birth_date: '1990-05-20',
  occupation: 'Engenheira',
  employment_type: 'clt' as const,
  monthly_income: 15000,
  residents: 2,
  has_pets: true,
};

const errorsOf = (r: { success: boolean; error?: { issues: { path: (string | number)[]; message: string }[] } }) =>
  Object.fromEntries((r.error?.issues ?? []).map((i) => [i.path.join('.'), i.message]));

describe('clientProfileSchema', () => {
  it('accepts a complete profile', () => {
    expect(clientProfileSchema.safeParse(profile).success).toBe(true);
  });

  it('accepts nullable optional fields', () => {
    const r = clientProfileSchema.safeParse({
      ...profile, cpf: null, birth_date: null, occupation: null, employment_type: null, monthly_income: null,
    });
    expect(r.success).toBe(true);
  });

  it('rejects an invalid CPF with a pt message', () => {
    const r = clientProfileSchema.safeParse({ ...profile, cpf: '111.111.111-11' });
    expect(r.success).toBe(false);
    expect(errorsOf(r)).toEqual({ cpf: 'CPF inválido' });
  });

  it('rejects a bad e-mail, short name and short phone', () => {
    const r = clientProfileSchema.safeParse({ ...profile, email: 'ana@', full_name: ' A ', phone: '9999' });
    expect(r.success).toBe(false);
    expect(errorsOf(r)).toMatchObject({
      email: 'E-mail inválido',
      full_name: 'Informe seu nome completo',
      phone: 'Telefone inválido',
    });
  });

  it('enforces residents 1..20 and date format', () => {
    expect(clientProfileSchema.safeParse({ ...profile, residents: 0 }).success).toBe(false);
    expect(clientProfileSchema.safeParse({ ...profile, residents: 21 }).success).toBe(false);
    expect(clientProfileSchema.safeParse({ ...profile, birth_date: '20/05/1990' }).success).toBe(false);
    expect(clientProfileSchema.safeParse({ ...profile, employment_type: 'freelancer' }).success).toBe(false);
  });

  it('trims the name', () => {
    const r = clientProfileSchema.parse({ ...profile, full_name: '  Ana Cliente  ' });
    expect(r.full_name).toBe('Ana Cliente');
  });
});

describe('applicationSchema', () => {
  const app = {
    listing_id: 1, intent: 'rent' as const, offered_price: 8500,
    guarantee_type: 'seguro_fianca' as const, move_in_date: '2026-11-01', message: 'Tenho um gato.',
  };

  it('accepts a valid proposta', () => {
    expect(applicationSchema.safeParse(app).success).toBe(true);
    expect(applicationSchema.safeParse({ ...app, guarantee_type: null, move_in_date: null, message: null }).success).toBe(true);
  });

  it('requires a positive integer price', () => {
    const r = applicationSchema.safeParse({ ...app, offered_price: 0 });
    expect(errorsOf(r)).toEqual({ offered_price: 'Informe um valor' });
    expect(applicationSchema.safeParse({ ...app, offered_price: 99.5 }).success).toBe(false);
  });

  it('rejects unknown intent / guarantee and over-long messages', () => {
    expect(applicationSchema.safeParse({ ...app, intent: 'lease' }).success).toBe(false);
    expect(applicationSchema.safeParse({ ...app, guarantee_type: 'cheque' }).success).toBe(false);
    expect(applicationSchema.safeParse({ ...app, message: 'x'.repeat(2001) }).success).toBe(false);
    expect(applicationSchema.safeParse({ ...app, listing_id: -1 }).success).toBe(false);
  });
});

describe('inquirySchema / visitSchema', () => {
  it('validate the contact + visit forms', () => {
    expect(inquirySchema.safeParse({ name: 'Ana', email: 'a@b.co', message: 'Quero visitar', property_id: 1, intent: 'rent' }).success).toBe(true);
    expect(inquirySchema.safeParse({ name: 'A', email: 'a@b.co', message: 'oi', property_id: null, intent: null }).success).toBe(false);

    const visit = {
      listing_id: 1, starts_at: '2026-10-10T14:00:00-03:00', visitor_name: 'Ana',
      visitor_email: 'a@b.co', visitor_phone: null, notes: null,
    };
    expect(visitSchema.safeParse(visit).success).toBe(true);
    expect(visitSchema.safeParse({ ...visit, starts_at: '2026-10-10 14:00' }).success).toBe(false);
  });
});
