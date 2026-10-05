-- RLS smoke tests for 0012 (and the client-relevant bits of 0001/0010).
-- Run after stubs.sql + all migrations, as a superuser, with ON_ERROR_STOP.
-- Every assertion raises an exception on failure → psql exits non-zero.
--
-- Users:  admin  aaaaaaaa-…-0001   (realtor, in app_admins)
--         u1     11111111-…-0001   (client)
--         u2     22222222-…-0002   (another client)

\set ON_ERROR_STOP on

-- ─── Fixtures (superuser) ─────────────────────────────────────────────
insert into auth.users (id, email) values
    ('aaaaaaaa-0000-0000-0000-000000000001', 'broker@regla.test'),
    ('11111111-0000-0000-0000-000000000001', 'ana@cliente.test'),
    ('22222222-0000-0000-0000-000000000002', 'bruno@cliente.test');
insert into public.app_admins (user_id) values ('aaaaaaaa-0000-0000-0000-000000000001');

insert into public.listings (id, slug, title, city, country, neighborhood, type, price, currency, area_m2, summary, description, status)
values (900, 'draft-test', 'Draft test', 'São Paulo', 'Brazil', 'Pinheiros', 'apartment', 5000, 'BRL', 50, 's', 'd', 'draft');

-- ─── u1: client happy path + forbidden actions ────────────────────────
begin;
select set_config('request.jwt.claim.sub', '11111111-0000-0000-0000-000000000001', true);
set local role authenticated;
do $$
declare n int; app record; ok boolean;
begin
    assert not public.is_admin(), 'u1 must not be admin';

    -- listings: live readable, draft hidden; photos readable
    select count(*) into n from public.listings;
    assert n = 20, format('u1 should see 20 live listings, saw %s', n);
    select count(*) into n from public.listings where id = 900;
    assert n = 0, 'u1 must not see draft listing';
    perform 1 from public.listing_photos limit 1;  -- no error = readable

    -- profile: own insert ok (upsert path), other user's denied
    insert into public.client_profiles (user_id, full_name, email, phone, cpf, monthly_income, residents)
    values (auth.uid(), 'Ana Cliente', 'ana@cliente.test', '11999990000', '529.982.247-25', 15000, 2)
    on conflict (user_id) do update set full_name = excluded.full_name;
    update public.client_profiles set occupation = 'Engenheira' where user_id = auth.uid();
    select count(*) into n from public.client_profiles;
    assert n = 1, 'u1 sees exactly own profile';

    ok := false;
    begin
        insert into public.client_profiles (user_id, full_name, email, phone)
        values ('22222222-0000-0000-0000-000000000002', 'Fake', 'x@x.test', '11999990000');
    exception when insufficient_privilege then ok := true;
    end;
    assert ok, 'u1 must not create a profile for u2';

    -- application: own insert ok, creates a CRM inquiry
    insert into public.applications (listing_id, user_id, intent, offered_price, guarantee_type, move_in_date, message)
    values (1, auth.uid(), 'rent', 8500, 'seguro_fianca', '2026-11-01', 'Tenho um gato.')
    returning * into app;
    assert app.status = 'submitted', 'default status submitted';
    assert app.inquiry_id is not null, 'insert trigger must link an inquiry';

    insert into public.applications (listing_id, user_id, intent, offered_price)
    values (2, auth.uid(), 'buy', 4000000);

    -- cannot self-approve on insert
    ok := false;
    begin
        insert into public.applications (listing_id, user_id, intent, offered_price, status)
        values (1, auth.uid(), 'rent', 9000, 'approved');
    exception when insufficient_privilege then ok := true;
    end;
    assert ok, 'u1 must not insert an approved application';

    -- cannot apply on behalf of someone else
    ok := false;
    begin
        insert into public.applications (listing_id, user_id, intent, offered_price)
        values (1, 'aaaaaaaa-0000-0000-0000-000000000001', 'rent', 9000);
    exception when insufficient_privilege or foreign_key_violation then ok := true;
    end;
    assert ok, 'u1 must not insert an application for another user';

    -- cannot change status to approved
    ok := false;
    begin
        update public.applications set status = 'approved' where id = app.id;
    exception when insufficient_privilege then ok := true;
    end;
    assert ok, 'u1 must not approve own application';

    -- cannot sneak other edits in alongside a withdraw
    ok := false;
    begin
        update public.applications set status = 'withdrawn', offered_price = 1 where id = app.id;
    exception when insufficient_privilege then ok := true;
    end;
    assert ok, 'u1 must not edit price while withdrawing';

    -- cannot edit other columns at all
    ok := false;
    begin
        update public.applications set reviewer_note = 'self' where id = app.id;
    exception when insufficient_privilege then ok := true;
    end;
    assert ok, 'u1 must not set reviewer_note';

    -- withdraw works
    update public.applications set status = 'withdrawn' where id = app.id;
    select count(*) into n from public.applications where id = app.id and status = 'withdrawn';
    assert n = 1, 'u1 can withdraw own application';

    -- inquiries are admin-only to read, but clients may insert (contact form)
    select count(*) into n from public.inquiries;
    assert n = 0, 'u1 must not read inquiries';
    insert into public.inquiries (name, email, message, property_id, intent)
    values ('Ana', 'ana@cliente.test', 'Quero visitar', 1, 'rent');

    -- book a visit (0010 visitor policy) and read it back
    insert into public.showings (listing_id, starts_at, type, status, created_by, visitor_name, visitor_email)
    values (1, now() + interval '2 days', 'private', 'scheduled', auth.uid(), 'Ana', 'ana@cliente.test');
    select count(*) into n from public.showings where created_by = auth.uid();
    assert n = 1, 'u1 sees own visit';

    -- favorites
    insert into public.favorites (user_id, listing_id) values (auth.uid(), 1), (auth.uid(), 2)
    on conflict do nothing;
    delete from public.favorites where user_id = auth.uid() and listing_id = 2;
    select count(*) into n from public.favorites;
    assert n = 1, 'u1 favorites';

    -- documents: storage object + row in own folder
    insert into storage.objects (bucket_id, name, owner)
    values ('client-documents', auth.uid()::text || '/cpf/1700000000000-cpf.pdf', auth.uid());
    insert into public.client_documents (user_id, application_id, kind, filename, storage_path, mime_type, size_bytes)
    values (auth.uid(), app.id, 'cpf', 'cpf.pdf', auth.uid()::text || '/cpf/1700000000000-cpf.pdf', 'application/pdf', 1234);
    select count(*) into n from storage.objects where bucket_id = 'client-documents';
    assert n = 1, 'u1 sees own storage object';

    ok := false;
    begin
        insert into storage.objects (bucket_id, name)
        values ('client-documents', '22222222-0000-0000-0000-000000000002/cpf/evil.pdf');
    exception when insufficient_privilege then ok := true;
    end;
    assert ok, 'u1 must not upload into another user folder';

    ok := false;
    begin
        insert into public.client_documents (user_id, kind, filename, storage_path, mime_type, size_bytes)
        values (auth.uid(), 'cpf', 'x.pdf', '22222222-0000-0000-0000-000000000002/cpf/x.pdf', 'application/pdf', 1);
    exception when insufficient_privilege then ok := true;
    end;
    assert ok, 'u1 must not register a document path outside own folder';

    -- realtor-only tables stay closed
    select count(*) into n from public.lead_notes;
    assert n = 0, 'u1 must not read lead notes';
end $$;
commit;

-- ─── u2: sees none of u1's data ───────────────────────────────────────
begin;
select set_config('request.jwt.claim.sub', '22222222-0000-0000-0000-000000000002', true);
set local role authenticated;
do $$
declare n int;
begin
    select count(*) into n from public.client_profiles;   assert n = 0, 'u2 must not see u1 profile';
    select count(*) into n from public.applications;      assert n = 0, 'u2 must not see u1 applications';
    select count(*) into n from public.client_documents;  assert n = 0, 'u2 must not see u1 documents';
    select count(*) into n from public.favorites;         assert n = 0, 'u2 must not see u1 favorites';
    select count(*) into n from storage.objects where bucket_id = 'client-documents';
    assert n = 0, 'u2 must not see u1 storage objects';
    select count(*) into n from public.showings where type = 'private';
    assert n = 0, 'u2 must not see u1 private visits';

    update public.applications set status = 'withdrawn';
    get diagnostics n = row_count;
    assert n = 0, 'u2 must not update u1 applications';
    delete from public.client_documents;
    get diagnostics n = row_count;
    assert n = 0, 'u2 must not delete u1 documents';
end $$;
commit;

-- ─── admin (realtor): sees everything, reviews ────────────────────────
begin;
select set_config('request.jwt.claim.sub', 'aaaaaaaa-0000-0000-0000-000000000001', true);
set local role authenticated;
do $$
declare n int; app_id bigint; inq record;
begin
    assert public.is_admin(), 'admin must be admin';
    select count(*) into n from public.client_profiles;   assert n = 1, 'admin sees profiles';
    select count(*) into n from public.applications;      assert n = 2, format('admin sees 2 applications, saw %s', n);
    select count(*) into n from public.client_documents;  assert n = 1, 'admin sees documents';
    select count(*) into n from storage.objects where bucket_id = 'client-documents';
    assert n = 1, 'admin sees client storage objects';
    select count(*) into n from public.listings;          assert n = 21, 'admin sees drafts too';

    -- the shape fetchApplicationsForReview embeds (applications → client_profiles FK)
    select count(*) into n
    from public.applications a join public.client_profiles p on p.user_id = a.user_id;
    assert n = 2, 'applications join client_profiles';

    -- auto-created leads
    select i.* into inq from public.inquiries i
    join public.applications a on a.inquiry_id = i.id
    where a.listing_id = 1;
    assert inq.stage = 'offer', 'lead stage offer';
    assert inq.email = 'ana@cliente.test', 'lead email from profile';
    assert inq.name = 'Ana Cliente', 'lead name from profile';
    assert inq.property_id = 1 and inq.intent = 'rent', 'lead property/intent';
    assert inq.message like 'Proposta #%aluguel%8.500%', format('lead message: %s', inq.message);

    -- approve the buy proposta → lead closed_won
    select id into app_id from public.applications where listing_id = 2;
    update public.applications
       set status = 'approved', reviewer_note = 'ok', reviewed_by = auth.uid()
     where id = app_id;
    select i.stage into inq from public.inquiries i join public.applications a on a.inquiry_id = i.id where a.id = app_id;
    assert inq.stage = 'closed_won', 'approval closes the lead as won';
end $$;
commit;

-- ─── u1 can't withdraw after approval ─────────────────────────────────
begin;
select set_config('request.jwt.claim.sub', '11111111-0000-0000-0000-000000000001', true);
set local role authenticated;
do $$
declare ok boolean := false;
begin
    begin
        update public.applications set status = 'withdrawn' where listing_id = 2;
    exception when insufficient_privilege then ok := true;
    end;
    assert ok, 'u1 must not withdraw an approved application';
end $$;
commit;

-- ─── anon: still reads live listings, nothing private ─────────────────
begin;
set local role anon;
do $$
declare n int;
begin
    select count(*) into n from public.listings;  assert n = 20, 'anon sees live listings';
    begin
        select count(*) into n from public.client_profiles;
        assert n = 0, 'anon must not read profiles';
    exception when insufficient_privilege then null;
    end;
end $$;
commit;

-- ─── Catalog checks (superuser) ───────────────────────────────────────
do $$
declare n int;
begin
    select count(*) into n from pg_publication_tables
    where pubname = 'supabase_realtime' and schemaname = 'public' and tablename in ('applications', 'inquiries');
    assert n = 2, 'applications + inquiries in supabase_realtime';
    assert (select not public from storage.buckets where id = 'client-documents'), 'client-documents is private';
end $$;

select 'RLS smoke tests passed' as result;
