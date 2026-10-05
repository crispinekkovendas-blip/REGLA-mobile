#!/usr/bin/env bash
# Apply every migration (twice, to prove idempotency) to a throwaway Postgres
# with Supabase stubs, then run the RLS smoke tests.
#
# Uses standard libpq env vars: PGHOST PGPORT PGUSER PGPASSWORD PGDATABASE.
# Must connect as a superuser (CI: the postgres:16 service container).
#   PGHOST=localhost PGUSER=postgres PGPASSWORD=postgres bash supabase/test/run.sh
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MIGRATIONS="$HERE/../migrations"
PSQL=(psql -X -q -v ON_ERROR_STOP=1)

DB="${PGDATABASE:-postgres}"
TEST_DB="regla_test_$$"

echo "› creating database $TEST_DB"
"${PSQL[@]}" -d "$DB" -c "create database $TEST_DB"
cleanup() { "${PSQL[@]}" -d "$DB" -c "drop database if exists $TEST_DB with (force)" >/dev/null 2>&1 || true; }
trap cleanup EXIT

run() { PGOPTIONS='--client-min-messages=warning' "${PSQL[@]}" -d "$TEST_DB" -f "$1"; }

echo "› supabase stubs"
run "$HERE/stubs.sql"

for pass in 1 2; do
  echo "› migrations (pass $pass)"
  for f in "$MIGRATIONS"/*.sql; do
    echo "  · $(basename "$f")"
    run "$f" >/dev/null
  done
done

echo "› RLS smoke tests"
run "$HERE/rls_smoke.sql"
echo "✓ database checks passed"
