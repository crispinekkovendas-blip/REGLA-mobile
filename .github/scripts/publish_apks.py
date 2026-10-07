#!/usr/bin/env python3
"""Publish the freshly built APKs to imoveisregla.com.br/app.

1. Writes android/dist/manifest.json (version, build time, commit, sizes).
2. Asks the website for signed Supabase Storage upload URLs
   (POST /api/app-release, Bearer = the run's GitHub Actions OIDC token; the
   site verifies it was issued to REGLA-mobile@main — no stored secret).
3. PUTs every file straight to Storage (APKs are too big for a Vercel function).
   The manifest goes last, so the page never points at half-uploaded APKs.

Env: RELEASE_BEARER (OIDC JWT), APP_VERSION_CODE, GITHUB_SHA, GITHUB_SERVER_URL,
     GITHUB_REPOSITORY, GITHUB_RUN_ID, RELEASE_ENDPOINT (optional).
"""
import json
import os
import pathlib
import sys
import urllib.request
from datetime import datetime, timezone

ENDPOINT = os.environ.get("RELEASE_ENDPOINT", "https://imoveisregla.com.br/api/app-release")
DIST = pathlib.Path(__file__).resolve().parents[2] / "android" / "dist"

files = {p.name: p for p in sorted(DIST.glob("*/*.apk"))}
if not files:
    sys.exit(f"no APKs under {DIST}")

code = int(os.environ.get("APP_VERSION_CODE", "1"))
manifest = {
    "version": f"0.1.{code}",
    "versionCode": code,
    "builtAt": datetime.now(timezone.utc).isoformat(timespec="seconds"),
    "commit": os.environ.get("GITHUB_SHA", "local")[:7],
    "runUrl": "{}/{}/actions/runs/{}".format(
        os.environ.get("GITHUB_SERVER_URL", "https://github.com"),
        os.environ.get("GITHUB_REPOSITORY", ""),
        os.environ.get("GITHUB_RUN_ID", ""),
    ),
    "files": {name: {"size": p.stat().st_size} for name, p in files.items()},
}
manifest_path = DIST / "manifest.json"
manifest_path.write_text(json.dumps(manifest, indent=2))
files["manifest.json"] = manifest_path

req = urllib.request.Request(
    ENDPOINT,
    data=json.dumps({"files": list(files)}).encode(),
    headers={
        "Authorization": f"Bearer {os.environ['RELEASE_BEARER']}",
        "Content-Type": "application/json",
        "User-Agent": "regla-mobile-ci",
    },
    method="POST",
)
with urllib.request.urlopen(req, timeout=60) as res:
    uploads = json.load(res)["uploads"]

order = [n for n in files if n != "manifest.json"] + ["manifest.json"]
for name in order:
    path = files[name]
    ctype = "application/json" if name.endswith(".json") else "application/vnd.android.package-archive"
    put = urllib.request.Request(
        uploads[name],
        data=path.read_bytes(),
        headers={"Content-Type": ctype, "x-upsert": "true", "cache-control": "max-age=60"},
        method="PUT",
    )
    with urllib.request.urlopen(put, timeout=600) as res:
        print(f"uploaded {name} ({path.stat().st_size // 1024} KB) -> HTTP {res.status}")

print(f"published v{manifest['version']} -> https://imoveisregla.com.br/app")
