// Form-state <-> shared zod schema adapters. Forms hold strings (masked input);
// these helpers turn them into the typed payloads the shared API expects and
// map zod issues onto per-field error messages.
import type { ZodError } from 'zod';
import {
  applicationSchema, clientProfileSchema, maskCpf, maskPhoneBR,
  type ApplicationInput, type ApplicationIntent, type ClientProfile, type ClientProfileInput,
  type EmploymentType, type GuaranteeType,
} from '@regla/shared';

export type FieldErrors<K extends string = string> = Partial<Record<K, string>>;

export function zodFieldErrors<K extends string>(err: ZodError): FieldErrors<K> {
  const out: FieldErrors<K> = {};
  for (const issue of err.issues) {
    const key = String(issue.path[0] ?? '_') as K;
    if (!out[key]) out[key] = issue.message;
  }
  return out;
}

export const onlyDigits = (s: string): string => s.replace(/\D/g, '');

/** "1234567" → "1.234.567" (for money inputs without the R$ prefix). */
export function maskMoney(input: string): string {
  const d = onlyDigits(input).replace(/^0+(?=\d)/, '').slice(0, 12);
  return d.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

/** "25121990" → "25/12/1990" */
export function maskDateBR(input: string): string {
  const d = onlyDigits(input).slice(0, 8);
  if (d.length <= 2) return d;
  if (d.length <= 4) return `${d.slice(0, 2)}/${d.slice(2)}`;
  return `${d.slice(0, 2)}/${d.slice(2, 4)}/${d.slice(4)}`;
}

/** "25/12/1990" → "1990-12-25" (null if empty, undefined if invalid). */
export function brDateToIso(input: string): string | null | undefined {
  const d = onlyDigits(input);
  if (!d) return null;
  if (d.length !== 8) return undefined;
  const day = Number(d.slice(0, 2));
  const month = Number(d.slice(2, 4));
  const year = Number(d.slice(4));
  const dt = new Date(year, month - 1, day);
  if (dt.getFullYear() !== year || dt.getMonth() !== month - 1 || dt.getDate() !== day || year < 1900) return undefined;
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

export function isoToBrDate(iso: string | null | undefined): string {
  if (!iso) return '';
  const [y, m, d] = iso.slice(0, 10).split('-');
  return y && m && d ? `${d}/${m}/${y}` : '';
}

// ─── Profile ("meu cadastro") ─────────────────────────────────────────

export interface ProfileFormValues {
  full_name: string;
  email: string;
  phone: string;
  cpf: string;
  birth_date: string;      // DD/MM/AAAA
  occupation: string;
  employment_type: EmploymentType | null;
  monthly_income: string;  // masked digits
  residents: number;
  has_pets: boolean;
}

export type ProfileField = keyof ProfileFormValues;

export function emptyProfileForm(email = '', fullName = ''): ProfileFormValues {
  return {
    full_name: fullName, email, phone: '', cpf: '', birth_date: '', occupation: '',
    employment_type: null, monthly_income: '', residents: 1, has_pets: false,
  };
}

export function profileToForm(p: ClientProfile | null, fallbackEmail = '', fallbackName = ''): ProfileFormValues {
  if (!p) return emptyProfileForm(fallbackEmail, fallbackName);
  return {
    full_name: p.full_name ?? fallbackName,
    email: p.email || fallbackEmail,
    phone: maskPhoneBR(p.phone ?? ''),
    cpf: maskCpf(p.cpf ?? ''),
    birth_date: isoToBrDate(p.birth_date),
    occupation: p.occupation ?? '',
    employment_type: p.employment_type,
    monthly_income: p.monthly_income != null ? maskMoney(String(p.monthly_income)) : '',
    residents: p.residents || 1,
    has_pets: !!p.has_pets,
  };
}

export type ValidationResult<T, K extends string> =
  | { ok: true; data: T; errors: FieldErrors<K> }
  | { ok: false; data: null; errors: FieldErrors<K> };

/**
 * Validate the cadastro form with the shared `clientProfileSchema`.
 * `strict` (used by the proposal wizard) additionally requires CPF and income.
 */
export function validateProfileForm(v: ProfileFormValues, strict = false): ValidationResult<ClientProfileInput, ProfileField> {
  const birth = brDateToIso(v.birth_date);
  const cpfDigits = onlyDigits(v.cpf);
  const incomeDigits = onlyDigits(v.monthly_income);
  const candidate = {
    full_name: v.full_name,
    email: v.email,
    phone: onlyDigits(v.phone),
    cpf: cpfDigits ? cpfDigits : null,
    birth_date: birth === undefined ? 'invalid' : birth,
    occupation: v.occupation.trim() ? v.occupation.trim() : null,
    employment_type: v.employment_type,
    monthly_income: incomeDigits ? Number(incomeDigits) : null,
    residents: v.residents,
    has_pets: v.has_pets,
  };
  const res = clientProfileSchema.safeParse(candidate);
  const errors: FieldErrors<ProfileField> = res.success ? {} : zodFieldErrors<ProfileField>(res.error);
  if (birth === undefined) errors.birth_date = 'Data inválida (use DD/MM/AAAA)';
  if (strict) {
    if (!cpfDigits) errors.cpf = errors.cpf ?? 'Informe seu CPF';
    if (!incomeDigits || Number(incomeDigits) <= 0) errors.monthly_income = 'Informe sua renda mensal';
    if (!v.employment_type) errors.employment_type = 'Selecione sua ocupação';
  }
  if (res.success && Object.keys(errors).length === 0) return { ok: true, data: res.data, errors };
  return { ok: false, data: null, errors };
}

// ─── Proposal ("proposta") ────────────────────────────────────────────

export interface ProposalFormValues {
  intent: ApplicationIntent;
  offered_price: string;   // masked digits
  guarantee_type: GuaranteeType | null;
  move_in_date: string;    // DD/MM/AAAA
  message: string;
}

export type ProposalField = keyof ProposalFormValues;

export function validateProposalForm(
  listingId: number, v: ProposalFormValues, today: Date = new Date(),
): ValidationResult<ApplicationInput, ProposalField> {
  const move = brDateToIso(v.move_in_date);
  const price = Number(onlyDigits(v.offered_price) || '0');
  const candidate = {
    listing_id: listingId,
    intent: v.intent,
    offered_price: price,
    guarantee_type: v.intent === 'rent' ? v.guarantee_type : null,
    move_in_date: move === undefined ? 'invalid' : move,
    message: v.message.trim() ? v.message.trim() : null,
  };
  const res = applicationSchema.safeParse(candidate);
  const errors: FieldErrors<ProposalField> = res.success ? {} : zodFieldErrors<ProposalField>(res.error);
  if (move === undefined) errors.move_in_date = 'Data inválida (use DD/MM/AAAA)';
  else if (move) {
    const t = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    if (new Date(`${move}T00:00:00`) < t) errors.move_in_date = 'A data precisa ser hoje ou depois';
  }
  if (v.intent === 'rent' && !v.guarantee_type) errors.guarantee_type = 'Escolha uma garantia';
  if (res.success && Object.keys(errors).length === 0) return { ok: true, data: res.data, errors };
  return { ok: false, data: null, errors };
}
