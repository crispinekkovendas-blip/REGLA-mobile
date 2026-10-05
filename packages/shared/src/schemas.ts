import { z } from 'zod';
import { isValidCpf } from './format';

export const clientProfileSchema = z.object({
  full_name: z.string().trim().min(3, 'Informe seu nome completo'),
  email: z.string().trim().email('E-mail inválido'),
  phone: z.string().refine((v) => v.replace(/\D/g, '').length >= 10, 'Telefone inválido'),
  cpf: z
    .string()
    .nullable()
    .refine((v) => !v || isValidCpf(v), 'CPF inválido'),
  birth_date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Use AAAA-MM-DD').nullable(),
  occupation: z.string().trim().nullable(),
  employment_type: z
    .enum(['clt', 'autonomo', 'empresario', 'servidor', 'aposentado', 'estudante', 'outro'])
    .nullable(),
  monthly_income: z.number().int().nonnegative().nullable(),
  residents: z.number().int().min(1).max(20),
  has_pets: z.boolean(),
});
export type ClientProfileInput = z.infer<typeof clientProfileSchema>;

export const applicationSchema = z.object({
  listing_id: z.number().int().positive(),
  intent: z.enum(['rent', 'buy']),
  offered_price: z.number().int().positive('Informe um valor'),
  guarantee_type: z
    .enum(['seguro_fianca', 'fiador', 'caucao', 'titulo_capitalizacao', 'sem_garantia'])
    .nullable(),
  move_in_date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).nullable(),
  message: z.string().trim().max(2000).nullable(),
});
export type ApplicationInput = z.infer<typeof applicationSchema>;

export const inquirySchema = z.object({
  name: z.string().trim().min(2),
  email: z.string().trim().email(),
  message: z.string().trim().min(5),
  property_id: z.number().int().positive().nullable(),
  intent: z.string().nullable(),
});
export type InquiryInput = z.infer<typeof inquirySchema>;

export const visitSchema = z.object({
  listing_id: z.number().int().positive(),
  starts_at: z.string().datetime({ offset: true }),
  visitor_name: z.string().trim().min(2),
  visitor_email: z.string().trim().email(),
  visitor_phone: z.string().nullable(),
  notes: z.string().nullable(),
});
export type VisitInput = z.infer<typeof visitSchema>;
