import type {
  ApplicationStatus, InquiryStage, ListingStatus, ListingType, Priority, ShowingStatus,
} from '@regla/shared';
import type { Tone } from '../theme';

export const LISTING_STATUSES: readonly ListingStatus[] = ['live', 'draft', 'sold', 'withdrawn'];

export const LISTING_STATUS_LABEL: Record<ListingStatus, string> = {
  live: 'Publicado',
  draft: 'Rascunho',
  sold: 'Vendido/Alugado',
  withdrawn: 'Retirado',
};

export const LISTING_STATUS_TONE: Record<ListingStatus, Tone> = {
  live: 'ok', draft: 'neutral', sold: 'info', withdrawn: 'danger',
};

export const LISTING_TYPE_LABEL: Record<ListingType, string> = {
  apartment: 'Apartamento', house: 'Casa', commercial: 'Comercial', land: 'Terreno',
};

export const PRIORITIES: readonly Priority[] = ['high', 'medium', 'low'];
export const PRIORITY_LABEL: Record<Priority, string> = { high: 'Alta', medium: 'Média', low: 'Baixa' };
export const PRIORITY_TONE: Record<Priority, Tone> = { high: 'danger', medium: 'warn', low: 'neutral' };

export const STAGE_TONE: Record<InquiryStage, Tone> = {
  inbox: 'coral', qualified: 'info', showing: 'warn', offer: 'navy', closed_won: 'ok', closed_lost: 'neutral',
};

export const APPLICATION_STATUS_TONE: Record<ApplicationStatus, Tone> = {
  submitted: 'coral', under_review: 'info', docs_requested: 'warn', approved: 'ok', rejected: 'danger', withdrawn: 'neutral',
};

export const SHOWING_STATUS_TONE: Record<ShowingStatus, Tone> = {
  scheduled: 'warn', confirmed: 'info', attended: 'ok', no_show: 'danger', cancelled: 'neutral',
};

export const INTENT_LABEL: Record<string, string> = { rent: 'Locação', buy: 'Compra' };

export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return '?';
  const first = parts[0]?.[0] ?? '';
  const last = parts.length > 1 ? parts[parts.length - 1]?.[0] ?? '' : '';
  return (first + last).toUpperCase();
}

export function maskCpfPartial(cpf: string | null): string {
  if (!cpf) return '—';
  const d = cpf.replace(/\D/g, '');
  if (d.length !== 11) return cpf;
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
}

export function formatBytes(n: number): string {
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${Math.round(n / 1024)} KB`;
  return `${(n / 1024 / 1024).toFixed(1).replace('.', ',')} MB`;
}
