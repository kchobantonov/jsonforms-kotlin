# Portable spec examples

Each folder contains one example as JSON: `data.json`, `schema.json`,
`uischema.json`, and optional `config.json` and `translations.json`.
`catalog.json` records display titles and the pinned source commit.

Edit these JSON files to change the Kotlin demo examples. The `:examples`
Gradle project generates its Kotlin catalog from them during compilation.
To refresh from a spec checkout or unpacked package, run
`node scripts/sync-spec-examples.mjs` from the repository root. See
[`examples/UPSTREAM.md`](../UPSTREAM.md) for provenance and exclusions.
