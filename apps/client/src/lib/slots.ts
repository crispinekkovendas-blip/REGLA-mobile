// Pure helpers for the visit scheduler. No React, no network — unit-tested.

export interface VisitDay {
  key: string;        // YYYY-MM-DD (local)
  date: Date;         // local midnight
  weekday: string;    // "seg"
  dayNumber: string;  // "07"
  month: string;      // "out"
  isToday: boolean;
}

export interface TimeSlot {
  label: string;      // "09:30"
  startsAt: Date;
  iso: string;        // ISO-8601 with offset (Z), accepted by visitSchema
  disabled: boolean;  // in the past / too soon
}

const WEEKDAYS = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'];
const MONTHS = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];

const pad = (n: number) => String(n).padStart(2, '0');

export const dayKey = (d: Date): string => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

/** The next `count` calendar days starting today (local time). */
export function nextDays(now: Date = new Date(), count = 14): VisitDay[] {
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  return Array.from({ length: count }, (_, i) => {
    const date = new Date(start.getFullYear(), start.getMonth(), start.getDate() + i);
    return {
      key: dayKey(date),
      date,
      weekday: i === 0 ? 'hoje' : WEEKDAYS[date.getDay()],
      dayNumber: pad(date.getDate()),
      month: MONTHS[date.getMonth()],
      isToday: i === 0,
    };
  });
}

export interface SlotOptions {
  startHour?: number;   // first slot, default 09:00
  endHour?: number;     // last slot (inclusive), default 18:00
  stepMinutes?: number; // default 30
  leadMinutes?: number; // minimum notice before a slot, default 60
}

/**
 * Time slots for a given day, 09:00–18:00 every 30 minutes (inclusive → 19 slots).
 * Slots earlier than `now + leadMinutes` are returned with `disabled: true`.
 */
export function timeSlots(day: Date, now: Date = new Date(), opts: SlotOptions = {}): TimeSlot[] {
  const { startHour = 9, endHour = 18, stepMinutes = 30, leadMinutes = 60 } = opts;
  const cutoff = now.getTime() + leadMinutes * 60_000;
  const slots: TimeSlot[] = [];
  for (let m = startHour * 60; m <= endHour * 60; m += stepMinutes) {
    const startsAt = new Date(day.getFullYear(), day.getMonth(), day.getDate(), Math.floor(m / 60), m % 60);
    slots.push({
      label: `${pad(Math.floor(m / 60))}:${pad(m % 60)}`,
      startsAt,
      iso: startsAt.toISOString(),
      disabled: startsAt.getTime() < cutoff,
    });
  }
  return slots;
}

/** "qui, 09 out · 14:30" */
export function formatVisitDate(iso: string): string {
  const d = new Date(iso);
  return `${WEEKDAYS[d.getDay()]}, ${pad(d.getDate())} ${MONTHS[d.getMonth()]} · ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** "09/10/2026" */
export function formatDateBR(iso: string): string {
  const d = iso.length === 10 ? new Date(`${iso}T12:00:00`) : new Date(iso);
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
}
