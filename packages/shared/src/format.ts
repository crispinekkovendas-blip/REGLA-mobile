import type { Currency, ApplicationStatus, InquiryStage, ShowingStatus, DocumentKind, GuaranteeType, EmploymentType } from './types';

export type Locale = 'pt' | 'en';

const LOCALE_TAG: Record<Locale, string> = { pt: 'pt-BR', en: 'en-US' };

/** Locale-aware price, no decimals. formatPrice(1850000,'BRL','pt') → "R$ 1.850.000" */
export function formatPrice(amount: number, currency: Currency, locale: Locale = 'pt'): string {
  return new Intl.NumberFormat(LOCALE_TAG[locale], {
    style: 'currency',
    currency,
    maximumFractionDigits: 0,
  })
    .format(amount)
    .replace(/ /g, ' ');
}

export function formatArea(m2: number, locale: Locale = 'pt'): string {
  return `${new Intl.NumberFormat(LOCALE_TAG[locale]).format(m2)} m²`;
}

/** Listing reference code shown to clients and brokers, e.g. "RG-0042". */
export const listingRef = (id: number): string => `RG-${String(id).padStart(4, '0')}`;

/** Strip non-digits and validate a Brazilian CPF (with check digits). */
export function isValidCpf(input: string): boolean {
  const cpf = input.replace(/\D/g, '');
  if (cpf.length !== 11 || /^(\d)\1{10}$/.test(cpf)) return false;
  const digit = (len: number) => {
    let sum = 0;
    for (let i = 0; i < len; i++) sum += Number(cpf[i]) * (len + 1 - i);
    const r = (sum * 10) % 11;
    return r === 10 ? 0 : r;
  };
  return digit(9) === Number(cpf[9]) && digit(10) === Number(cpf[10]);
}

export function maskCpf(input: string): string {
  const d = input.replace(/\D/g, '').slice(0, 11);
  return d
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1-$2');
}

export function maskPhoneBR(input: string): string {
  const d = input.replace(/\D/g, '').slice(0, 11);
  if (d.length <= 2) return d.length ? `(${d}` : '';
  if (d.length <= 7) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
  return `(${d.slice(0, 2)}) ${d.slice(2, d.length - 4)}-${d.slice(-4)}`;
}

export const APPLICATION_STATUS_LABEL: Record<ApplicationStatus, string> = {
  submitted: 'Enviada',
  under_review: 'Em análise',
  docs_requested: 'Documentos pendentes',
  approved: 'Aprovada',
  rejected: 'Recusada',
  withdrawn: 'Cancelada',
};

export const STAGE_LABEL: Record<InquiryStage, string> = {
  inbox: 'Novo',
  qualified: 'Qualificado',
  showing: 'Visita',
  offer: 'Proposta',
  closed_won: 'Fechado',
  closed_lost: 'Perdido',
};

export const SHOWING_STATUS_LABEL: Record<ShowingStatus, string> = {
  scheduled: 'Agendada',
  confirmed: 'Confirmada',
  attended: 'Realizada',
  no_show: 'Não compareceu',
  cancelled: 'Cancelada',
};

export const DOCUMENT_KIND_LABEL: Record<DocumentKind, string> = {
  rg_cnh: 'RG ou CNH',
  cpf: 'CPF',
  comprovante_renda: 'Comprovante de renda',
  comprovante_residencia: 'Comprovante de residência',
  extrato_bancario: 'Extrato bancário',
  imposto_renda: 'Declaração de IR',
  outro: 'Outro',
};

export const GUARANTEE_LABEL: Record<GuaranteeType, string> = {
  seguro_fianca: 'Seguro fiança',
  fiador: 'Fiador',
  caucao: 'Caução',
  titulo_capitalizacao: 'Título de capitalização',
  sem_garantia: 'Sem garantia',
};

export const EMPLOYMENT_LABEL: Record<EmploymentType, string> = {
  clt: 'CLT',
  autonomo: 'Autônomo',
  empresario: 'Empresário',
  servidor: 'Servidor público',
  aposentado: 'Aposentado',
  estudante: 'Estudante',
  outro: 'Outro',
};

/** QuintoAndar-style affordability rule: rent should be ≤ 30% of household income. */
export function affordability(
  monthlyRent: number,
  monthlyIncome: number | null,
): 'ok' | 'tight' | 'over' | 'unknown' {
  if (!monthlyIncome || monthlyIncome <= 0) return 'unknown';
  const ratio = monthlyRent / monthlyIncome;
  if (ratio <= 0.3) return 'ok';
  if (ratio <= 0.4) return 'tight';
  return 'over';
}
