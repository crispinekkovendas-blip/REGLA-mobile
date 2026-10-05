import type { Locale } from './format';

export const BROKER_WHATSAPP = '5511932210855'; // +55 11 93221-0855

export const waUrl = (text: string, phone: string = BROKER_WHATSAPP): string =>
  `https://wa.me/${phone}?text=${encodeURIComponent(text)}`;

export const waInterest = (title: string, ref: string, locale: Locale = 'pt'): string =>
  waUrl(
    locale === 'pt'
      ? `Olá! Tenho interesse no imóvel "${title}" (ref. ${ref}). Pode me passar mais informações?`
      : `Hello! I'm interested in the listing "${title}" (ref. ${ref}). Could you share more details?`,
  );

/** Realtor → client: opens a chat with the client's phone (BR digits, with or without +55). */
export const waToClient = (phone: string, text: string): string => {
  let d = phone.replace(/\D/g, '');
  if (d.length <= 11) d = `55${d}`;
  return waUrl(text, d);
};
