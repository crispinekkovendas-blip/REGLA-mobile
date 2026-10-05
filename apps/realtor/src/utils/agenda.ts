/** Local-time YYYY-MM-DD key for a date. */
export function dayKey(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

export function startOfDay(d: Date): Date {
  const s = new Date(d);
  s.setHours(0, 0, 0, 0);
  return s;
}

export function addDays(d: Date, n: number): Date {
  const s = new Date(d);
  s.setDate(s.getDate() + n);
  return s;
}

export interface DayGroup<T> {
  key: string;
  date: Date;
  items: T[];
}

/**
 * Bucket items into `days` consecutive local days starting at `from` (inclusive).
 * Every day is present (empty days have `items: []`), items within a day are
 * sorted by start time, and items outside the window are dropped.
 */
export function groupByDay<T extends { starts_at: string }>(items: readonly T[], from: Date, days = 7): DayGroup<T>[] {
  const start = startOfDay(from);
  const groups: DayGroup<T>[] = [];
  const index = new Map<string, DayGroup<T>>();
  for (let i = 0; i < days; i++) {
    const date = addDays(start, i);
    const g: DayGroup<T> = { key: dayKey(date), date, items: [] };
    groups.push(g);
    index.set(g.key, g);
  }
  for (const it of items) {
    const g = index.get(dayKey(new Date(it.starts_at)));
    if (g) g.items.push(it);
  }
  for (const g of groups) g.items.sort((a, b) => Date.parse(a.starts_at) - Date.parse(b.starts_at));
  return groups;
}

const WEEKDAYS = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'];
const MONTHS = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];

/** "Hoje", "Amanhã" or "qua, 8 out". */
export function dayLabel(date: Date, now = new Date()): string {
  const diff = Math.round((startOfDay(date).getTime() - startOfDay(now).getTime()) / 86_400_000);
  if (diff === 0) return 'Hoje';
  if (diff === 1) return 'Amanhã';
  if (diff === -1) return 'Ontem';
  return `${WEEKDAYS[date.getDay()]}, ${date.getDate()} ${MONTHS[date.getMonth()]}`;
}

export function shortDate(date: Date): string {
  return `${date.getDate()} ${MONTHS[date.getMonth()]}`;
}

export function timeLabel(iso: string): string {
  const d = new Date(iso);
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

/** "agora", "há 5 min", "há 3 h", "há 2 d", or a short date. */
export function relativeTime(iso: string, now = new Date()): string {
  const s = Math.max(0, Math.round((now.getTime() - Date.parse(iso)) / 1000));
  if (s < 60) return 'agora';
  if (s < 3600) return `há ${Math.floor(s / 60)} min`;
  if (s < 86_400) return `há ${Math.floor(s / 3600)} h`;
  if (s < 7 * 86_400) return `há ${Math.floor(s / 86_400)} d`;
  return shortDate(new Date(iso));
}

/** "dd/mm/aaaa" from a YYYY-MM-DD date string (no timezone shift). */
export function formatIsoDate(ymd: string | null): string {
  if (!ymd) return '—';
  const [y, m, d] = ymd.slice(0, 10).split('-');
  return d && m && y ? `${d}/${m}/${y}` : ymd;
}
