import { describe, expect, it } from 'vitest';
import {
  affordability, formatArea, formatPrice, isValidCpf, listingRef, maskCpf, maskPhoneBR,
} from '../format';

describe('formatPrice', () => {
  it('formats BRL in pt-BR with no decimals and a plain space', () => {
    expect(formatPrice(1850000, 'BRL', 'pt')).toBe('R$ 1.850.000');
    expect(formatPrice(1850000, 'BRL')).toBe('R$ 1.850.000'); // pt is the default
  });

  it('formats USD in en-US', () => {
    expect(formatPrice(3850000, 'USD', 'en')).toBe('$3,850,000');
  });

  it('rounds away decimals', () => {
    expect(formatPrice(8500.7, 'BRL', 'pt')).toBe('R$ 8.501');
  });

  it('never emits non-breaking spaces', () => {
    expect(formatPrice(2400000, 'EUR', 'pt')).not.toMatch(/ /);
  });
});

describe('formatArea', () => {
  it('uses locale grouping', () => {
    expect(formatArea(120000, 'pt')).toBe('120.000 m²');
    expect(formatArea(120000, 'en')).toBe('120,000 m²');
  });
});

describe('listingRef', () => {
  it('zero-pads to 4 digits', () => {
    expect(listingRef(42)).toBe('RG-0042');
    expect(listingRef(1)).toBe('RG-0001');
    expect(listingRef(12345)).toBe('RG-12345');
  });
});

describe('isValidCpf', () => {
  it('accepts a known valid CPF, masked or not', () => {
    expect(isValidCpf('529.982.247-25')).toBe(true);
    expect(isValidCpf('52998224725')).toBe(true);
  });

  it('rejects wrong check digits', () => {
    expect(isValidCpf('529.982.247-24')).toBe(false);
    expect(isValidCpf('529.982.247-15')).toBe(false);
  });

  it('rejects repeated digits and wrong lengths', () => {
    expect(isValidCpf('111.111.111-11')).toBe(false);
    expect(isValidCpf('00000000000')).toBe(false);
    expect(isValidCpf('5299822472')).toBe(false);
    expect(isValidCpf('')).toBe(false);
  });
});

describe('maskCpf', () => {
  it('masks progressively while typing', () => {
    expect(maskCpf('529')).toBe('529');
    expect(maskCpf('5299')).toBe('529.9');
    expect(maskCpf('5299822')).toBe('529.982.2');
    expect(maskCpf('52998224725')).toBe('529.982.247-25');
  });

  it('strips junk and caps at 11 digits', () => {
    expect(maskCpf('529.982.247-25999')).toBe('529.982.247-25');
    expect(maskCpf('abc')).toBe('');
  });
});

describe('maskPhoneBR', () => {
  it('masks mobile and landline numbers', () => {
    expect(maskPhoneBR('')).toBe('');
    expect(maskPhoneBR('1')).toBe('(1');
    expect(maskPhoneBR('11')).toBe('(11');
    expect(maskPhoneBR('11932')).toBe('(11) 932');
    expect(maskPhoneBR('1132210855')).toBe('(11) 3221-0855');
    expect(maskPhoneBR('11932210855')).toBe('(11) 93221-0855');
    expect(maskPhoneBR('+55 (11) 93221-0855')).toBe('(55) 11932-2108'); // only the first 11 digits are kept
  });
});

describe('affordability (rent ≤ 30% of income)', () => {
  it('returns unknown without income', () => {
    expect(affordability(3000, null)).toBe('unknown');
    expect(affordability(3000, 0)).toBe('unknown');
    expect(affordability(3000, -1)).toBe('unknown');
  });

  it('applies the 30% / 40% thresholds inclusively', () => {
    expect(affordability(3000, 10000)).toBe('ok');      // exactly 30%
    expect(affordability(3001, 10000)).toBe('tight');
    expect(affordability(4000, 10000)).toBe('tight');   // exactly 40%
    expect(affordability(4001, 10000)).toBe('over');
  });
});
