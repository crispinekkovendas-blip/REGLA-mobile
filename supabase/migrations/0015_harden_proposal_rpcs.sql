-- =====================================================================
-- REGLA · 0015 · Harden proposal RPCs (follow-up to 0014)
-- Paste into Supabase SQL Editor → Run.  Idempotent.
--
-- 1. Logged-out callers (anon) can no longer execute the proposal RPCs.
-- 2. The RPCs check auth first and only "see" proposals the caller may act
--    on (realtor: any; client: own). Anyone else gets the same
--    'application not found' as a missing id, so ids can't be probed.
-- =====================================================================

-- Locks and returns an application the caller may act on.
create or replace function public.proposal_lock(p_application_id bigint)
returns public.applications
language plpgsql security definer set search_path = public as $$
declare
    app public.applications;
begin
    if auth.uid() is null then
        raise exception 'not authenticated' using errcode = '42501';
    end if;
    select * into app from public.applications
     where id = p_application_id
       and (public.is_admin() or user_id = auth.uid())
       for update;
    if not found then
        raise exception 'application not found' using errcode = 'P0002';
    end if;
    return app;
end $$;
revoke all on function public.proposal_lock(bigint) from public, anon;

create or replace function public.proposal_counter(
    p_application_id bigint,
    p_price          bigint,
    p_message        text default null,
    p_guarantee_type text default null,
    p_move_in_date   date default null)
returns public.application_offers
language plpgsql security definer set search_path = public as $$
declare
    app   public.applications;
    party text;
    last  public.application_offers;
    offer public.application_offers;
begin
    app := public.proposal_lock(p_application_id);
    party := public.proposal_party(app);
    if app.status not in ('submitted', 'under_review', 'negotiating') or app.awaiting is distinct from party then
        raise exception 'it is not your turn to counter' using errcode = '42501';
    end if;
    if p_price is null or p_price <= 0 then
        raise exception 'price must be positive' using errcode = '22023';
    end if;

    select * into last from public.application_offers
     where application_id = p_application_id order by created_at desc, id desc limit 1;

    insert into public.application_offers (application_id, author, author_id, price, guarantee_type, move_in_date, message)
    values (p_application_id, party, auth.uid(), p_price,
            coalesce(p_guarantee_type, last.guarantee_type),
            coalesce(p_move_in_date, last.move_in_date),
            nullif(trim(p_message), ''))
    returning * into offer;

    update public.applications
       set status = 'negotiating',
           awaiting = case party when 'client' then 'realtor' else 'client' end
     where id = p_application_id;
    return offer;
end $$;

create or replace function public.proposal_accept(p_application_id bigint)
returns public.applications
language plpgsql security definer set search_path = public as $$
declare
    app  public.applications;
    party text;
    last public.application_offers;
    res  public.applications;
begin
    app := public.proposal_lock(p_application_id);
    party := public.proposal_party(app);
    if app.status not in ('submitted', 'under_review', 'negotiating') or app.awaiting is distinct from party then
        raise exception 'it is not your turn to accept' using errcode = '42501';
    end if;
    select * into last from public.application_offers
     where application_id = p_application_id order by created_at desc, id desc limit 1;
    if last.author = party then
        raise exception 'cannot accept your own offer' using errcode = '42501';
    end if;

    update public.applications
       set status = 'accepted',
           agreed_price = last.price,
           guarantee_type = coalesce(last.guarantee_type, guarantee_type),
           move_in_date = coalesce(last.move_in_date, move_in_date),
           reviewed_by = case when party = 'realtor' then auth.uid() else reviewed_by end
     where id = p_application_id
    returning * into res;
    return res;
end $$;

create or replace function public.proposal_decline(p_application_id bigint, p_note text default null)
returns public.applications
language plpgsql security definer set search_path = public as $$
declare
    app public.applications;
    party text;
    res public.applications;
begin
    app := public.proposal_lock(p_application_id);
    party := public.proposal_party(app);
    if app.status in ('approved', 'rejected', 'withdrawn') then
        raise exception 'application is already closed' using errcode = '42501';
    end if;
    if party = 'realtor' then
        update public.applications
           set status = 'rejected', reviewer_note = nullif(trim(p_note), ''), reviewed_by = auth.uid()
         where id = p_application_id
        returning * into res;
    else
        update public.applications set status = 'withdrawn'
         where id = p_application_id
        returning * into res;
    end if;
    return res;
end $$;

create or replace function public.proposal_docs_sent(p_application_id bigint)
returns public.applications
language plpgsql security definer set search_path = public as $$
declare
    app public.applications;
    party text;
    res public.applications;
begin
    app := public.proposal_lock(p_application_id);
    party := public.proposal_party(app);
    if party <> 'client' or app.status not in ('accepted', 'docs_requested') then
        raise exception 'documents can only be sent after acceptance' using errcode = '42501';
    end if;
    update public.applications set status = 'docs_review'
     where id = p_application_id
    returning * into res;
    return res;
end $$;

-- Supabase grants EXECUTE on new public functions to anon by default: take it back.
revoke all on function public.proposal_party(public.applications) from public, anon;
revoke all on function public.proposal_counter(bigint, bigint, text, text, date) from public, anon;
revoke all on function public.proposal_accept(bigint) from public, anon;
revoke all on function public.proposal_decline(bigint, text) from public, anon;
revoke all on function public.proposal_docs_sent(bigint) from public, anon;
grant execute on function public.proposal_counter(bigint, bigint, text, text, date) to authenticated;
grant execute on function public.proposal_accept(bigint) to authenticated;
grant execute on function public.proposal_decline(bigint, text) to authenticated;
grant execute on function public.proposal_docs_sent(bigint) to authenticated;
