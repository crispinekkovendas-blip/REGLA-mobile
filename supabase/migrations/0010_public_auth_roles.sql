-- =====================================================================
-- REGLA · Slice: public auth + role separation (visit-booking login wall)
-- Paste into Supabase SQL Editor → Run.  Idempotent.
-- =====================================================================
-- Public visitors can now sign in (Google OAuth; WhatsApp OTP later).
-- Every pre-existing `to authenticated` policy assumed the only logged-in
-- user was the admin — public signups break that assumption. This migration:
--   1. app_admins table + is_admin() helper
--   2. seeds every existing auth user as admin (pre-public-auth = founder)
--   3. rewrites all broad `to authenticated` policies to require is_admin()
--   4. adds narrow policies for public visitors

-- ─── 1. Admin registry ─────────────────────────────────────────────────
create table if not exists public.app_admins (
    user_id    uuid primary key references auth.users(id) on delete cascade,
    created_at timestamptz not null default now()
);
alter table public.app_admins enable row level security;
-- No policies on purpose: only the service role and the security-definer
-- function below can read it.

create or replace function public.is_admin(uid uuid default auth.uid())
returns boolean
language sql stable security definer set search_path = public
as $$
    select exists (select 1 from public.app_admins where user_id = uid);
$$;
revoke all on function public.is_admin(uuid) from public;
grant execute on function public.is_admin(uuid) to authenticated, anon, service_role;

-- ─── 2. Seed ───────────────────────────────────────────────────────────
insert into public.app_admins (user_id)
select id from auth.users
on conflict (user_id) do nothing;

-- ─── 3. Rewrite broad authenticated policies → admin-only ──────────────

-- 0001 · listings / listing_photos / inquiries
drop policy if exists "listings: auth reads all" on public.listings;
create policy "listings: auth reads all" on public.listings
    for select to authenticated using (public.is_admin());

drop policy if exists "listings: auth writes" on public.listings;
create policy "listings: auth writes" on public.listings
    for all to authenticated using (public.is_admin()) with check (public.is_admin());

drop policy if exists "photos: auth writes" on public.listing_photos;
create policy "photos: auth writes" on public.listing_photos
    for all to authenticated using (public.is_admin()) with check (public.is_admin());

drop policy if exists "inquiries: auth manage" on public.inquiries;
create policy "inquiries: auth manage" on public.inquiries
    for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- 0002 · lead_notes
drop policy if exists "lead_notes: auth manage" on public.lead_notes;
create policy "lead_notes: auth manage" on public.lead_notes
    for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- 0003 · showings / showing_rsvps
drop policy if exists "showings: auth manage" on public.showings;
create policy "showings: auth manage" on public.showings
    for all to authenticated using (public.is_admin()) with check (public.is_admin());

drop policy if exists "rsvps: auth manage" on public.showing_rsvps;
create policy "rsvps: auth manage" on public.showing_rsvps
    for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- 0004 · agents
drop policy if exists "agents: auth reads all" on public.agents;
create policy "agents: auth reads all" on public.agents
    for select to authenticated using (public.is_admin());

drop policy if exists "agents: auth writes" on public.agents;
create policy "agents: auth writes" on public.agents
    for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- 0005 · lead_assignments
drop policy if exists "lead_assignments: auth manage" on public.lead_assignments;
create policy "lead_assignments: auth manage" on public.lead_assignments
    for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- 0006 · deal_documents
drop policy if exists "deal_documents: auth manage" on public.deal_documents;
create policy "deal_documents: auth manage" on public.deal_documents
    for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- 0007 · listing_videos
drop policy if exists "listing_videos: auth manage" on public.listing_videos;
create policy "listing_videos: auth manage" on public.listing_videos
    for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- 0008 · saved_searches / search_digests
drop policy if exists "saved_searches: auth all" on public.saved_searches;
create policy "saved_searches: auth all" on public.saved_searches
    for all to authenticated using (public.is_admin()) with check (public.is_admin());

drop policy if exists "search_digests: auth all" on public.search_digests;
create policy "search_digests: auth all" on public.search_digests
    for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- 0009 · lead_events
drop policy if exists "lead_events: auth read" on public.lead_events;
create policy "lead_events: auth read" on public.lead_events
    for select to authenticated using (public.is_admin());

-- Storage policies (0001 / 0004 / 0006 / 0007)
drop policy if exists "listing-photos: auth manage" on storage.objects;
create policy "listing-photos: auth manage" on storage.objects
    for all to authenticated
    using (bucket_id = 'listing-photos' and public.is_admin())
    with check (bucket_id = 'listing-photos' and public.is_admin());

drop policy if exists "agent-photos: auth manage" on storage.objects;
create policy "agent-photos: auth manage" on storage.objects
    for all to authenticated
    using (bucket_id = 'agent-photos' and public.is_admin())
    with check (bucket_id = 'agent-photos' and public.is_admin());

drop policy if exists "deal-documents: auth read" on storage.objects;
create policy "deal-documents: auth read" on storage.objects
    for select to authenticated
    using (bucket_id = 'deal-documents' and public.is_admin());

drop policy if exists "deal-documents: auth manage" on storage.objects;
create policy "deal-documents: auth manage" on storage.objects
    for all to authenticated
    using (bucket_id = 'deal-documents' and public.is_admin())
    with check (bucket_id = 'deal-documents' and public.is_admin());

drop policy if exists "listing-videos: auth manage" on storage.objects;
create policy "listing-videos: auth manage" on storage.objects
    for all to authenticated
    using (bucket_id = 'listing-videos' and public.is_admin())
    with check (bucket_id = 'listing-videos' and public.is_admin());

-- ─── 4. Visitor policies ───────────────────────────────────────────────
-- Booked server-side via service role today, but these make a future
-- "my visits" page work without another migration.
drop policy if exists "showings: visitor books own" on public.showings;
create policy "showings: visitor books own" on public.showings
    for insert to authenticated
    with check (created_by = auth.uid() and type = 'private' and status = 'scheduled');

drop policy if exists "showings: visitor reads own" on public.showings;
create policy "showings: visitor reads own" on public.showings
    for select to authenticated
    using (created_by = auth.uid());

-- Logged-in visitors keep the abilities anonymous visitors already have.
drop policy if exists "lead_events: authed insert" on public.lead_events;
create policy "lead_events: authed insert" on public.lead_events
    for insert to authenticated with check (true);

drop policy if exists "saved_searches: authed insert" on public.saved_searches;
create policy "saved_searches: authed insert" on public.saved_searches
    for insert to authenticated with check (true);

-- ─── Done. Verify ──────────────────────────────────────────────────────
-- select public.is_admin('<founder-uuid>');   -- expect true
-- select count(*) from public.app_admins;     -- expect 1
