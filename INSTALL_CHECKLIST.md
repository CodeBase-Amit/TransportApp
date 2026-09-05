# TransportApp2 — Install & Run Checklist (first office)

## What you need
- The signed release APK: pp/build/outputs/apk/release/app-release.apk
  (built with .\gradlew.bat :app:assembleRelease)
- An Android 7.0+ phone (minSdk 24)

## Install (sideload)
1. Copy the APK to the phone (USB, Drive, WhatsApp — any channel).
2. On the phone, tap the APK; allow "Install from this source" when asked.
3. Open **TransportApp** from the launcher.

## First run
1. **Sign in** — Continue with Google (mock identity while the backend auth ships).
2. **Register a company** — name, head office, GSTIN (optional), branch + code.
   The branch's bilty numbering series is created automatically.
3. **Book the first bilty** — the dashboard's empty state is the entry point.
4. Everything after that is the normal loop: bilty -> print 4 copies -> challan ->
   dispatch -> POD -> billing -> CSV export. **All of it works offline.**

## What works offline (no internet needed)
- Booking, printing, sharing (PDF), challans, trips, POD + signature,
  billing/receipts/statements, all reports, CSV export, masters CRUD.

## What needs a connection (online tier)
- Masters refresh from the server, real server-leased numbers,
  the outbox drain (Account & data -> Sync now).

## Signing (release)
- Keystore: C:\Users\Lenovo\haulmate-keystore\haulmate-release.jks (BACK THIS UP —
  losing it means you can never update the installed app).
- Credentials live in key.properties (gitignored).
- applicationId: com.haulmate.transportapp — final; do not change after public installs.