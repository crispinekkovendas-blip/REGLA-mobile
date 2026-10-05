-- =====================================================================
-- REGLA · 0013 · Mobile client self-service updates
-- Paste into Supabase SQL Editor → Run.  Idempotent.
--
-- 1. Clients can cancel their own visit requests from the app.
-- 2. Clients can link their own uploaded documents to their own proposta
--    (the app uploads during the wizard, before the application exists).
-- =====================================================================

-- ─── 1. Showings: visitor cancels own ─────────────────────────────────
drop policy if exists "showings: visitor cancels own" on public.showings;
create policy "showings: visitor cancels own" on public.showings
    for update to authenticated
    using (created_by = auth.uid() and status in ('scheduled', 'confirmed'))
    with check (created_by = auth.uid() and status = 'cancelled');

-- ─── 2. Client documents: owner links to own application ──────────────
drop policy if exists "client_documents: owner update" on public.client_documents;
create policy "client_documents: owner update" on public.client_documents
    for update to authenticated
    using (user_id = auth.uid())
    with check (
        user_id = auth.uid()
        and storage_path like auth.uid()::text || '/%'
        and (application_id is null or exists (
            select 1 from public.applications a
            where a.id = application_id and a.user_id = auth.uid()))
    );

grant update on public.client_documents to authenticated;
