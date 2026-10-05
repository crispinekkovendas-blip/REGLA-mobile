# REGLA mobile

Two Expo (SDK 57) apps on top of the existing REGLA Supabase project (the same one behind imoveisregla.com.br):

| App | Package | For |
|---|---|---|
| `apps/client` | `@regla/client` | Clients: browse live listings, favorites, book visits, fill "meu cadastro", send propostas (applications) and upload documents |
| `apps/realtor` | `@regla/realtor` | Realtors (= `is_admin()` users): leads/CRM, agenda, review propostas and documents, listing status |

```
apps/client         Expo Router app (client)
apps/realtor        Expo Router app (realtor)
packages/shared     @regla/shared: row types, zod schemas, formatters, WhatsApp links, Supabase data layer (+ vitest tests)
supabase/migrations 0001–0011 copied from the REGLA web repo (same filenames) + 0012_mobile_client.sql
supabase/test       Supabase stubs + RLS smoke tests run in CI against postgres:16
```

## Run locally

```bash
npm install                                   # once, at the repo root (npm workspaces)
cp apps/client/.env.example apps/client/.env  # fill EXPO_PUBLIC_SUPABASE_URL / _ANON_KEY
cp apps/client/.env.example apps/realtor/.env
npm run start -w @regla/client                # or @regla/realtor
```

Scan the QR code with **Expo Go** (Android/iOS), or press `w` for the web build.

Checks: `npm run typecheck`, `npm test` (all workspaces), or per package e.g. `npm test -w @regla/shared`.

## Database: apply migration 0012

0001–0011 are already live. Only `0012_mobile_client.sql` is new:

1. Supabase Dashboard → SQL Editor → New query.
2. Paste the whole of `supabase/migrations/0012_mobile_client.sql` → Run. It is idempotent (safe to re-run).
3. Verify with the queries at the bottom of the file.

It adds `client_profiles`, `applications` (propostas), `client_documents`, `favorites`, the private `client-documents` storage bucket, RLS for all of them, and adds `applications` + `inquiries` to the `supabase_realtime` publication. Each new application automatically creates an `inquiries` row (stage `offer`) so it shows up in the web CRM; approving it moves that lead to `closed_won`. Clients can only withdraw their own open applications. A client must have a `client_profiles` row before submitting an application (FK).

Realtors are whoever is in `public.app_admins` (see 0010).

## CI (`.github/workflows/ci.yml`)

On push / PR to `main`, Node 24, `npm ci`:

- **shared**: `tsc --noEmit` + vitest for `packages/shared`.
- **app** (matrix: client, realtor): typecheck, jest, `expo export --platform web --platform android` with dummy Supabase env; the `dist/` is uploaded as the `<app>-dist` artifact.
- **database**: `postgres:16` service; `supabase/test/run.sh` loads Supabase stubs (`auth.users`, `auth.uid()`, `storage.*`, `supabase_realtime`, API roles), applies all migrations **twice** (idempotency) and runs `supabase/test/rls_smoke.sql` (owner/other-user/admin/anon RLS assertions, withdraw-only guard, proposta → inquiry trigger).

Run the DB checks yourself against any throwaway superuser Postgres:

```bash
PGHOST=localhost PGUSER=postgres PGPASSWORD=postgres bash supabase/test/run.sh
```

## EAS builds (Android APK)

`apps/*/eas.json` define `development` (dev client, needs `expo-dev-client`), `preview` (internal APK) and `production` (AAB) profiles.

One-time setup per app:

```bash
npm i -g eas-cli && eas login
cd apps/client && eas init      # writes extra.eas.projectId into app.json; repeat in apps/realtor
```

Then either build locally (`eas build --platform android --profile preview`) or add an `EXPO_TOKEN` repo secret (expo.dev → Account settings → Access tokens) and run **Actions → EAS build → Run workflow** (`.github/workflows/eas-build.yml`, manual only; skips cleanly when the secret is missing). The APK link appears on expo.dev when the build finishes.
