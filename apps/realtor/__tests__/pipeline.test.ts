import { countBy, countByStage, sortLeads, APPLICATION_STATUSES } from '../src/utils/pipeline';
import type { InquiryStage, Priority } from '@regla/shared';

const lead = (stage: InquiryStage, read = true) => ({ stage, read });

describe('countByStage', () => {
  it('returns zeros for an empty list', () => {
    expect(countByStage([])).toEqual({
      inbox: 0, qualified: 0, showing: 0, offer: 0, closed_won: 0, closed_lost: 0, all: 0, unread: 0,
    });
  });

  it('groups leads by pipeline stage and counts total + unread', () => {
    const c = countByStage([
      lead('inbox', false), lead('inbox', false), lead('inbox'),
      lead('qualified'), lead('showing', false), lead('offer'),
      lead('closed_won'), lead('closed_won'), lead('closed_lost'),
    ]);
    expect(c.inbox).toBe(3);
    expect(c.qualified).toBe(1);
    expect(c.showing).toBe(1);
    expect(c.offer).toBe(1);
    expect(c.closed_won).toBe(2);
    expect(c.closed_lost).toBe(1);
    expect(c.all).toBe(9);
    expect(c.unread).toBe(3);
  });

  it('ignores unknown stages', () => {
    const c = countByStage([lead('inbox'), { stage: 'bogus' as InquiryStage, read: false }]);
    expect(c.all).toBe(1);
    expect(c.unread).toBe(0);
  });
});

describe('countBy', () => {
  it('counts application statuses with every key present', () => {
    const c = countBy([{ s: 'submitted' }, { s: 'approved' }, { s: 'submitted' }] as const, (x) => x.s, APPLICATION_STATUSES);
    expect(c).toEqual({ submitted: 2, under_review: 0, docs_requested: 0, approved: 1, rejected: 0, withdrawn: 0 });
  });
});

describe('sortLeads', () => {
  it('puts unread first, then high priority, then most recent', () => {
    const mk = (id: number, read: boolean, priority: Priority, at: string) => ({ id, read, priority, last_activity_at: at });
    const sorted = sortLeads([
      mk(1, true, 'high', '2026-10-01T10:00:00Z'),
      mk(2, false, 'low', '2026-10-01T09:00:00Z'),
      mk(3, true, 'high', '2026-10-02T10:00:00Z'),
      mk(4, false, 'high', '2026-09-30T10:00:00Z'),
      mk(5, true, 'low', '2026-10-03T10:00:00Z'),
    ]);
    expect(sorted.map((l) => l.id)).toEqual([4, 2, 3, 1, 5]);
  });
});
