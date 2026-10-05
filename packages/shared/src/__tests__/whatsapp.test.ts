import { describe, expect, it } from 'vitest';
import { BROKER_WHATSAPP, waInterest, waToClient, waUrl } from '../whatsapp';

const textOf = (url: string) => decodeURIComponent(new URL(url).searchParams.get('text') ?? '');

describe('waUrl', () => {
  it('defaults to the broker number and encodes text', () => {
    const url = waUrl('Olá & bem-vindo? 100%');
    expect(url.startsWith(`https://wa.me/${BROKER_WHATSAPP}?text=`)).toBe(true);
    expect(url).toContain('Ol%C3%A1%20%26%20bem-vindo%3F%20100%25');
  });
});

describe('waInterest', () => {
  it('builds the pt message with title and ref', () => {
    const url = waInterest('Studio "Itaim" & Co', 'RG-0001');
    expect(url).toMatch(/^https:\/\/wa\.me\/5511932210855\?text=/);
    expect(url).not.toContain(' ');
    expect(new URL(url).searchParams.get('text')).toBe(
      'Olá! Tenho interesse no imóvel "Studio "Itaim" & Co" (ref. RG-0001). Pode me passar mais informações?',
    );
  });

  it('builds the en message', () => {
    expect(new URL(waInterest('Loft', 'RG-0009', 'en')).searchParams.get('text')).toBe(
      'Hello! I\'m interested in the listing "Loft" (ref. RG-0009). Could you share more details?',
    );
  });
});

describe('waToClient', () => {
  it('adds the 55 country code to local BR numbers', () => {
    expect(waToClient('(11) 93221-0855', 'oi')).toBe('https://wa.me/5511932210855?text=oi');
    expect(waToClient('1132210855', 'oi')).toBe('https://wa.me/551132210855?text=oi');
  });

  it('keeps numbers that already carry +55', () => {
    expect(waToClient('+55 11 93221-0855', 'oi')).toBe('https://wa.me/5511932210855?text=oi');
  });

  it('encodes the message', () => {
    const url = waToClient('11932210855', 'Sua proposta foi aprovada! 🎉');
    expect(textOf(url)).toBe('Sua proposta foi aprovada! 🎉');
    expect(url).not.toContain(' ');
  });
});
