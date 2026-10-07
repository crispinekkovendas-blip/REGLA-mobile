-- RLS / lifecycle smoke tests for 0014 (proposal negotiation).
-- Runs after rls_smoke.sql (reuses its users: admin, u1, u2).
-- Listings 3, 4, 5 are seed listings with no proposals yet.

\set ON_ERROR_STOP on

-- Runs a statement; true when it fails with insufficient_privilege (42501).
create or replace function public.test_expect_denied(sql text) returns boolean language plpgsql as $$
begin
    execute sql;
    return false;
exception when insufficient_privilege then
    return true;
end $$;
grant execute on function public.test_expect_denied(text) to authenticated;

-- True when a statement fails with no_data_found (P0002) — "not found", no existence leak.
create or replace function public.test_expect_hidden(sql text) returns boolean language plpgsql as $$
begin
    execute sql;
    return false;
exception when no_data_found then
    return true;
end $$;
grant execute on function public.test_expect_hidden(text) to authenticated;
grant execute on function public.test_expect_denied(text) to anon;

-- ─── u1 sends a proposta on listing 3 (no documents needed) ───────────
begin;
select set_config('request.jwt.claim.sub', '11111111-0000-0000-0000-000000000001', true);
set local role authenticated;
do $$
declare app record; n int;
begin
    insert into public.applications (listing_id, user_id, intent, offered_price, guarantee_type, message)
    values (3, auth.uid(), 'rent', 5000, 'seguro_fianca', 'Proposta inicial')
    returning * into app;
    assert app.awaiting = 'realtor', format('new proposta awaits realtor, got %s', app.awaiting);
    select count(*) into n from public.application_offers
     where application_id = app.id and author = 'client' and price = 5000;
    assert n = 1, 'opening offer recorded';

    assert public.test_expect_denied(format('select public.proposal_counter(%s, 4800)', app.id)),
        'u1 must not counter on the realtor''s turn';
    assert public.test_expect_denied(format('select public.proposal_accept(%s)', app.id)),
        'u1 must not accept own opening offer';
    assert public.test_expect_denied(format('select public.proposal_docs_sent(%s)', app.id)),
        'u1 must not send docs before acceptance';
    assert public.test_expect_denied(format('update public.applications set status = %L where id = %s', 'accepted', app.id)),
        'u1 must not set accepted directly';
    assert public.test_expect_denied(format(
        'insert into public.application_offers (application_id, author, price) values (%s, %L, 1)', app.id, 'realtor')),
        'u1 must not insert offers directly';
end $$;
commit;

-- ─── realtor counters ─────────────────────────────────────────────────
begin;
select set_config('request.jwt.claim.sub', 'aaaaaaaa-0000-0000-0000-000000000001', true);
set local role authenticated;
do $$
declare app record; o record;
begin
    select * into app from public.applications where listing_id = 3;
    o := public.proposal_counter(app.id, 5500, 'Proprietário pede 5.500');
    assert o.author = 'realtor' and o.price = 5500, 'realtor offer row';
    assert o.guarantee_type = 'seguro_fianca', 'counter inherits guarantee';
    select * into app from public.applications where id = app.id;
    assert app.status = 'negotiating' and app.awaiting = 'client',
        format('after realtor counter: %s/%s', app.status, app.awaiting);
    assert public.test_expect_denied(format('select public.proposal_counter(%s, 5600)', app.id)),
        'realtor must not counter twice in a row';
end $$;
commit;

-- ─── u2 can neither see nor act on it ─────────────────────────────────
select set_config('regla.test_app', (select id::text from public.applications where listing_id = 3), false);
begin;
select set_config('request.jwt.claim.sub', '22222222-0000-0000-0000-000000000002', true);
set local role authenticated;
do $$
declare n int;
begin
    select count(*) into n from public.application_offers;
    assert n = 0, 'u2 must not see u1 offers';
    assert public.test_expect_hidden(format('select public.proposal_accept(%s)', current_setting('regla.test_app'))),
        'u2 must not accept u1 proposta (and must not learn it exists)';
    assert public.test_expect_hidden('select public.proposal_accept(999999)'),
        'missing id looks the same as someone else''s';
end $$;
commit;

-- ─── u1 sees the timeline and counters back ───────────────────────────
begin;
select set_config('request.jwt.claim.sub', '11111111-0000-0000-0000-000000000001', true);
set local role authenticated;
do $$
declare app record; n int;
begin
    select * into app from public.applications where listing_id = 3;
    select count(*) into n from public.application_offers where application_id = app.id;
    assert n = 2, format('u1 sees 2 offers, saw %s', n);
    perform public.proposal_counter(app.id, 5200, 'Fecho em 5.200');
    select * into app from public.applications where id = app.id;
    assert app.awaiting = 'realtor', 'after client counter, realtor turn';
end $$;
commit;

-- ─── realtor accepts → agreed price locked ────────────────────────────
begin;
select set_config('request.jwt.claim.sub', 'aaaaaaaa-0000-0000-0000-000000000001', true);
set local role authenticated;
do $$
declare app record;
begin
    select * into app from public.applications where listing_id = 3;
    app := public.proposal_accept(app.id);
    assert app.status = 'accepted' and app.agreed_price = 5200 and app.awaiting = 'client',
        format('accepted: %s %s %s', app.status, app.agreed_price, app.awaiting);
end $$;
commit;

-- ─── u1 uploads documents and marks them sent ─────────────────────────
begin;
select set_config('request.jwt.claim.sub', '11111111-0000-0000-0000-000000000001', true);
set local role authenticated;
do $$
declare app record;
begin
    select * into app from public.applications where listing_id = 3;
    insert into public.client_documents (user_id, application_id, kind, filename, storage_path, mime_type, size_bytes)
    values (auth.uid(), app.id, 'rg_cnh', 'rg.pdf', auth.uid()::text || '/rg_cnh/1-rg.pdf', 'application/pdf', 10);
    app := public.proposal_docs_sent(app.id);
    assert app.status = 'docs_review' and app.awaiting = 'realtor', 'docs_review awaits realtor';
end $$;
commit;

-- ─── realtor asks for a correction → client re-sends → approve ────────
begin;
select set_config('request.jwt.claim.sub', 'aaaaaaaa-0000-0000-0000-000000000001', true);
set local role authenticated;
do $$
declare app record;
begin
    update public.applications set status = 'docs_requested', reviewer_note = 'Falta comprovante de renda'
     where listing_id = 3 returning * into app;
    assert app.awaiting = 'client', 'docs_requested awaits client';
end $$;
commit;

begin;
select set_config('request.jwt.claim.sub', '11111111-0000-0000-0000-000000000001', true);
set local role authenticated;
do $$
declare app record;
begin
    select * into app from public.applications where listing_id = 3;
    app := public.proposal_docs_sent(app.id);
    assert app.status = 'docs_review', 're-sent docs';
end $$;
commit;

begin;
select set_config('request.jwt.claim.sub', 'aaaaaaaa-0000-0000-0000-000000000001', true);
set local role authenticated;
do $$
declare app record; st text;
begin
    update public.applications set status = 'approved', reviewed_by = auth.uid()
     where listing_id = 3 returning * into app;
    assert app.awaiting is null, 'approved awaits nobody';
    select i.stage into st from public.inquiries i where i.id = app.inquiry_id;
    assert st = 'closed_won', 'approval closes the lead';
end $$;
commit;

-- ─── closed stays closed; decline paths ───────────────────────────────
begin;
select set_config('request.jwt.claim.sub', '11111111-0000-0000-0000-000000000001', true);
set local role authenticated;
do $$
declare app record;
begin
    select * into app from public.applications where listing_id = 3;
    assert public.test_expect_denied(format('select public.proposal_decline(%s)', app.id)),
        'closed proposta cannot be declined';
    insert into public.applications (listing_id, user_id, intent, offered_price)
    values (4, auth.uid(), 'rent', 3000);
    -- client decline = withdraw
    insert into public.applications (listing_id, user_id, intent, offered_price)
    values (5, auth.uid(), 'rent', 3000) returning * into app;
    app := public.proposal_decline(app.id);
    assert app.status = 'withdrawn' and app.awaiting is null, 'client decline → withdrawn';
end $$;
commit;

begin;
select set_config('request.jwt.claim.sub', 'aaaaaaaa-0000-0000-0000-000000000001', true);
set local role authenticated;
do $$
declare app record;
begin
    select * into app from public.applications where listing_id = 4;
    app := public.proposal_decline(app.id, 'Imóvel já alugado');
    assert app.status = 'rejected' and app.reviewer_note = 'Imóvel já alugado' and app.awaiting is null,
        'realtor decline → rejected';
end $$;
commit;

-- ─── anon cannot call the RPCs at all (0015) ──────────────────────────
begin;
set local role anon;
do $$
begin
    assert public.test_expect_denied('select public.proposal_accept(1)'), 'anon must not execute proposal_accept';
    assert public.test_expect_denied('select public.proposal_counter(1, 100)'), 'anon must not execute proposal_counter';
    assert public.test_expect_denied('select public.proposal_docs_sent(1)'), 'anon must not execute proposal_docs_sent';
end $$;
commit;

-- ─── Catalog ──────────────────────────────────────────────────────────
do $$
begin
    assert exists (select 1 from pg_publication_tables
                   where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'application_offers'),
        'application_offers in supabase_realtime';
end $$;

drop function public.test_expect_denied(text);
drop function public.test_expect_hidden(text);
select '0014 lifecycle tests passed' as result;
