import type { ApplicationStatus, InquiryStage, Priority } from '@regla/shared';

export const STAGES: readonly InquiryStage[] = ['inbox', 'qualified', 'showing', 'offer', 'closed_won', 'closed_lost'];

export const APPLICATION_STATUSES: readonly ApplicationStatus[] = [
  'submitted', 'under_review', 'docs_requested', 'approved', 'rejected', 'withdrawn',
];

export type StageCounts = Record<InquiryStage, number> & { all: number; unread: number };

/** Count leads per pipeline stage (+ total and unread). Unknown stages are ignored. */
export function countByStage(leads: readonly { stage: InquiryStage; read?: boolean }[]): StageCounts {
  const out = { inbox: 0, qualified: 0, showing: 0, offer: 0, closed_won: 0, closed_lost: 0, all: 0, unread: 0 } as StageCounts;
  for (const l of leads) {
    if (!(STAGES as readonly string[]).includes(l.stage)) continue;
    out[l.stage] += 1;
    out.all += 1;
    if (l.read === false) out.unread += 1;
  }
  return out;
}

/** Generic count-by-key helper (used for application status filter chips). */
export function countBy<T, K extends string>(items: readonly T[], key: (t: T) => K, keys: readonly K[]): Record<K, number> {
  const out = Object.fromEntries(keys.map((k) => [k, 0])) as Record<K, number>;
  for (const it of items) {
    const k = key(it);
    if (k in out) out[k] += 1;
  }
  return out;
}

const PRIORITY_RANK: Record<Priority, number> = { high: 0, medium: 1, low: 2 };

/** Unread first, then priority, then most recent activity. */
export function sortLeads<T extends { read: boolean; priority: Priority; last_activity_at: string }>(leads: readonly T[]): T[] {
  return [...leads].sort((a, b) =>
    Number(a.read) - Number(b.read)
    || PRIORITY_RANK[a.priority] - PRIORITY_RANK[b.priority]
    || b.last_activity_at.localeCompare(a.last_activity_at));
}
