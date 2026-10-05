// Row types mirroring the REGLA Supabase schema (REGLA/supabase/migrations 0001–0011)
// plus the mobile additions in REGLA-mobile/supabase/migrations/0012_mobile_client.sql.

export type ListingType = 'apartment' | 'house' | 'commercial' | 'land';
export type ListingStatus = 'draft' | 'live' | 'sold' | 'withdrawn';
export type Currency = 'USD' | 'EUR' | 'GBP' | 'BRL';

export interface Listing {
  id: number;
  slug: string;
  title: string;
  city: string;
  country: string;
  neighborhood: string;
  type: ListingType;
  price: number;
  currency: Currency;
  beds: number;
  baths: number;
  area_m2: number;
  area_ft2: number | null;
  tags: string[];
  palette: string;
  shape: string;
  summary: string;
  description: string;
  status: ListingStatus;
  featured: boolean;
  agent_id: number | null;
  created_at: string;
  updated_at: string;
}

export interface ListingPhoto {
  id: number;
  listing_id: number;
  storage_path: string;
  alt_text: string | null;
  position: number;
}

export interface ListingWithPhotos extends Listing {
  listing_photos: ListingPhoto[];
}

export type InquiryStage = 'inbox' | 'qualified' | 'showing' | 'offer' | 'closed_won' | 'closed_lost';
export type Priority = 'low' | 'medium' | 'high';

export interface Inquiry {
  id: number;
  name: string;
  email: string;
  intent: string | null;
  region: string | null;
  message: string;
  property_id: number | null;
  read: boolean;
  stage: InquiryStage;
  priority: Priority;
  last_activity_at: string;
  assigned_agent_id: number | null;
  created_at: string;
}

export interface LeadNote {
  id: number;
  inquiry_id: number;
  body: string;
  author_id: string | null;
  created_at: string;
}

export type ShowingType = 'private' | 'open_house';
export type ShowingStatus = 'scheduled' | 'confirmed' | 'attended' | 'no_show' | 'cancelled';

export interface Showing {
  id: number;
  listing_id: number;
  inquiry_id: number | null;
  type: ShowingType;
  starts_at: string;
  duration_minutes: number;
  status: ShowingStatus;
  visitor_name: string | null;
  visitor_email: string | null;
  visitor_phone: string | null;
  notes: string | null;
  created_by: string | null;
  created_at: string;
}

// ─── Mobile additions (0012) ──────────────────────────────────────────

export type EmploymentType =
  | 'clt' | 'autonomo' | 'empresario' | 'servidor' | 'aposentado' | 'estudante' | 'outro';

export interface ClientProfile {
  user_id: string;
  full_name: string;
  email: string;
  phone: string;
  cpf: string | null;
  birth_date: string | null;          // YYYY-MM-DD
  occupation: string | null;
  employment_type: EmploymentType | null;
  monthly_income: number | null;      // whole BRL
  residents: number;
  has_pets: boolean;
  created_at: string;
  updated_at: string;
}

export type ApplicationIntent = 'rent' | 'buy';
export type GuaranteeType =
  | 'seguro_fianca' | 'fiador' | 'caucao' | 'titulo_capitalizacao' | 'sem_garantia';
export type ApplicationStatus =
  | 'submitted' | 'under_review' | 'docs_requested' | 'approved' | 'rejected' | 'withdrawn';

/** A "proposta" — the client's formal offer/application on a listing (QuintoAndar-style). */
export interface Application {
  id: number;
  listing_id: number;
  user_id: string;
  intent: ApplicationIntent;
  offered_price: number;
  guarantee_type: GuaranteeType | null;
  move_in_date: string | null;
  message: string | null;
  status: ApplicationStatus;
  reviewer_note: string | null;
  reviewed_by: string | null;
  inquiry_id: number | null;
  created_at: string;
  updated_at: string;
}

export type DocumentKind =
  | 'rg_cnh' | 'cpf' | 'comprovante_renda' | 'comprovante_residencia'
  | 'extrato_bancario' | 'imposto_renda' | 'outro';

export interface ClientDocument {
  id: number;
  user_id: string;
  application_id: number | null;
  kind: DocumentKind;
  filename: string;
  storage_path: string;   // in bucket 'client-documents', always `${user_id}/...`
  mime_type: string;
  size_bytes: number;
  created_at: string;
}

export interface Favorite {
  user_id: string;
  listing_id: number;
  created_at: string;
}
