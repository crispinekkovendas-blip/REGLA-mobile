import { dayKey, dayLabel, groupByDay, relativeTime, formatIsoDate } from '../src/utils/agenda';

// Build ISO strings from *local* wall-clock times so tests are timezone-independent.
const at = (y: number, m: number, d: number, h: number, min = 0) => new Date(y, m - 1, d, h, min).toISOString();

describe('groupByDay', () => {
  const from = new Date(2026, 9, 4, 15, 30); // Sun 4 Oct 2026, mid-afternoon

  it('creates one bucket per day for the 7-day window, starting at local midnight', () => {
    const groups = groupByDay([], from, 7);
    expect(groups).toHaveLength(7);
    expect(groups.map((g) => g.key)).toEqual([
      '2026-10-04', '2026-10-05', '2026-10-06', '2026-10-07', '2026-10-08', '2026-10-09', '2026-10-10',
    ]);
    expect(groups.every((g) => g.items.length === 0)).toBe(true);
    expect(groups[0]!.date.getHours()).toBe(0);
  });

  it('puts showings in their local day, sorted by time, and drops out-of-window items', () => {
    const items = [
      { id: 1, starts_at: at(2026, 10, 5, 16) },
      { id: 2, starts_at: at(2026, 10, 5, 9, 30) },
      { id: 3, starts_at: at(2026, 10, 4, 8) },   // earlier today: still today
      { id: 4, starts_at: at(2026, 10, 10, 23, 59) },
      { id: 5, starts_at: at(2026, 10, 11, 0, 0) }, // day 8 → dropped
      { id: 6, starts_at: at(2026, 10, 3, 23, 0) }, // yesterday → dropped
    ];
    const groups = groupByDay(items, from, 7);
    const byKey = Object.fromEntries(groups.map((g) => [g.key, g.items.map((i) => i.id)]));
    expect(byKey['2026-10-04']).toEqual([3]);
    expect(byKey['2026-10-05']).toEqual([2, 1]);
    expect(byKey['2026-10-10']).toEqual([4]);
    expect(groups.reduce((n, g) => n + g.items.length, 0)).toBe(4);
  });

  it('respects a custom window length', () => {
    expect(groupByDay([{ starts_at: at(2026, 10, 6, 10) }], from, 2).flatMap((g) => g.items)).toHaveLength(0);
  });
});

describe('day helpers', () => {
  const now = new Date(2026, 9, 4, 12);
  it('labels today / tomorrow / other days in Portuguese', () => {
    expect(dayLabel(new Date(2026, 9, 4, 20), now)).toBe('Hoje');
    expect(dayLabel(new Date(2026, 9, 5, 1), now)).toBe('Amanhã');
    expect(dayLabel(new Date(2026, 9, 7), now)).toBe('qua, 7 out');
  });

  it('dayKey is local YYYY-MM-DD', () => {
    expect(dayKey(new Date(2026, 0, 9, 23, 59))).toBe('2026-01-09');
  });

  it('relativeTime', () => {
    expect(relativeTime(new Date(now.getTime() - 30_000).toISOString(), now)).toBe('agora');
    expect(relativeTime(new Date(now.getTime() - 5 * 60_000).toISOString(), now)).toBe('há 5 min');
    expect(relativeTime(new Date(now.getTime() - 3 * 3600_000).toISOString(), now)).toBe('há 3 h');
  });

  it('formatIsoDate', () => {
    expect(formatIsoDate('2026-11-01')).toBe('01/11/2026');
    expect(formatIsoDate(null)).toBe('—');
  });
});
