import { formatVisitDate, nextDays, timeSlots } from '@/lib/slots';

describe('visit slot generation', () => {
  const day = new Date(2026, 9, 10); // Sat 10 Oct 2026, local midnight

  it('returns 09:00–18:00 every 30 minutes (19 slots)', () => {
    const slots = timeSlots(day, new Date(2026, 9, 1));
    expect(slots).toHaveLength(19);
    expect(slots[0].label).toBe('09:00');
    expect(slots[1].label).toBe('09:30');
    expect(slots[slots.length - 1].label).toBe('18:00');
    expect(slots.map((s) => s.label)).toContain('13:30');
    expect(slots.every((s) => !s.disabled)).toBe(true);
  });

  it('builds local start times and ISO strings accepted by visitSchema', () => {
    const [first] = timeSlots(day, new Date(2026, 9, 1));
    expect(first.startsAt.getHours()).toBe(9);
    expect(first.startsAt.getMinutes()).toBe(0);
    expect(first.iso).toBe(first.startsAt.toISOString());
    expect(first.iso).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/);
  });

  it('disables slots in the past or within the 1h lead time', () => {
    const now = new Date(2026, 9, 10, 11, 10); // 11:10 same day
    const slots = timeSlots(day, now);
    const enabled = slots.filter((s) => !s.disabled).map((s) => s.label);
    expect(enabled[0]).toBe('12:30'); // 12:10 cutoff → first free slot 12:30
    expect(slots.find((s) => s.label === '12:00')?.disabled).toBe(true);
    expect(enabled).toHaveLength(12); // 12:30 … 18:00
  });

  it('supports custom ranges', () => {
    const slots = timeSlots(day, new Date(2026, 0, 1), { startHour: 10, endHour: 12, stepMinutes: 60 });
    expect(slots.map((s) => s.label)).toEqual(['10:00', '11:00', '12:00']);
  });

  it('lists the next 14 days starting today, across month boundaries', () => {
    const days = nextDays(new Date(2026, 9, 25, 15, 0), 14);
    expect(days).toHaveLength(14);
    expect(days[0]).toMatchObject({ key: '2026-10-25', isToday: true, weekday: 'hoje' });
    expect(days[1].isToday).toBe(false);
    expect(days[7].key).toBe('2026-11-01');
    expect(days[7].month).toBe('nov');
    expect(days[13].key).toBe('2026-11-07');
  });

  it('formats a visit date in PT-BR', () => {
    const iso = new Date(2026, 9, 8, 14, 30).toISOString();
    expect(formatVisitDate(iso)).toBe('qui, 08 out · 14:30');
  });
});
