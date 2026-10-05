-- 0011: visitor session tracking on showings
-- Lets the public booking flow (Calendly-style calendar, 2026-08-10) cap
-- anonymous visitors at 2 active future bookings per regla_sid cookie.

alter table public.showings add column if not exists session_uuid uuid;

create index if not exists showings_session_uuid_idx
  on public.showings (session_uuid)
  where session_uuid is not null;
