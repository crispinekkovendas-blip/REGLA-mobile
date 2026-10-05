-- Minimal stand-in for the bits of a Supabase project the migrations rely on,
-- so they can be applied to a vanilla postgres:16 (CI) and RLS-tested.
-- NOT a migration — never run this against the real project.

do $$
begin
    if not exists (select 1 from pg_roles where rolname = 'anon')          then create role anon nologin noinherit; end if;
    if not exists (select 1 from pg_roles where rolname = 'authenticated') then create role authenticated nologin noinherit; end if;
    if not exists (select 1 from pg_roles where rolname = 'service_role')  then create role service_role nologin noinherit bypassrls; end if;
end $$;

create extension if not exists pgcrypto;   -- gen_random_bytes (0008)

-- ─── auth ─────────────────────────────────────────────────────────────
create schema if not exists auth;

create table if not exists auth.users (
    id          uuid primary key default gen_random_uuid(),
    email       text,
    created_at  timestamptz not null default now()
);

-- Same contract as Supabase: the JWT `sub` claim of the current request.
create or replace function auth.uid() returns uuid language sql stable as $$
    select nullif(
        coalesce(
            current_setting('request.jwt.claim.sub', true),
            (nullif(current_setting('request.jwt.claims', true), '')::jsonb ->> 'sub')
        ), ''
    )::uuid
$$;

-- ─── storage ──────────────────────────────────────────────────────────
create schema if not exists storage;

create table if not exists storage.buckets (
    id          text primary key,
    name        text not null unique,
    public      boolean default false,
    created_at  timestamptz default now()
);

create table if not exists storage.objects (
    id          uuid primary key default gen_random_uuid(),
    bucket_id   text references storage.buckets(id),
    name        text,
    owner       uuid,
    created_at  timestamptz default now(),
    unique (bucket_id, name)
);
alter table storage.objects enable row level security;

create or replace function storage.foldername(name text) returns text[] language plpgsql immutable as $$
declare _parts text[];
begin
    select string_to_array(name, '/') into _parts;
    return _parts[1:array_length(_parts, 1) - 1];
end $$;

-- ─── realtime ─────────────────────────────────────────────────────────
do $$
begin
    if not exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
        create publication supabase_realtime;
    end if;
end $$;

-- ─── privileges (Supabase grants API roles everything; RLS does the gating) ─
grant usage on schema public, auth, storage to anon, authenticated, service_role;
grant execute on function auth.uid() to anon, authenticated, service_role;
grant execute on function storage.foldername(text) to anon, authenticated, service_role;
grant all on all tables in schema storage to anon, authenticated, service_role;
alter default privileges in schema public grant all on tables    to anon, authenticated, service_role;
alter default privileges in schema public grant all on sequences to anon, authenticated, service_role;
alter default privileges in schema public grant all on functions to anon, authenticated, service_role;
