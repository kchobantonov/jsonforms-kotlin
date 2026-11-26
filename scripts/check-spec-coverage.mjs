import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const readJson = (path) => JSON.parse(readFileSync(resolve(path), 'utf8'));
const coverage = readJson('docs/spec-example-coverage.json');
const imported = readJson('examples/spec/catalog.json');
const source = readJson(process.env.JSONFORMS_SPEC_CATALOG ?? 'node_modules/@chobantonov/jsonforms-extended-spec/examples/catalog.json');
const sourceIds = source.map(({ id }) => id);
const importedIds = imported.examples.map(({ id }) => id);
const rows = coverage.examples;
const rowIds = rows.map(({ id }) => id);
const failures = [];
const duplicates = (ids) => ids.filter((id, index) => ids.indexOf(id) !== index);
for (const [name, ids] of [['source', sourceIds], ['imported', importedIds], ['coverage', rowIds]]) {
  if (duplicates(ids).length) failures.push(`Duplicate ${name} IDs: ${duplicates(ids).join(', ')}`);
}
for (const id of sourceIds.filter((id) => !rowIds.includes(id))) failures.push(`Unclassified source example: ${id}`);
for (const id of rowIds.filter((id) => !sourceIds.includes(id))) failures.push(`Coverage row absent from source: ${id}`);
for (const id of importedIds.filter((id) => !sourceIds.includes(id))) failures.push(`Imported example absent from source: ${id}`);
for (const row of rows) {
  const present = importedIds.includes(row.id);
  const importedState = ['imported-unverified', 'imported-partial', 'imported-verified'].includes(row.fixture);
  if (importedState !== present) failures.push(`Wrong fixture state for ${row.id}`);
  if (!['imported-unverified', 'imported-partial', 'imported-verified', 'missing-review', 'platform-exclusion'].includes(row.fixture)) failures.push(`Unknown fixture state for ${row.id}`);
  if (!row.reason) failures.push(`Missing reason for ${row.id}`);
}
if (coverage.specCommit !== imported.commit) failures.push('Coverage and imported catalog pin different spec commits');
if (coverage.sourceCatalogCount !== sourceIds.length || coverage.importedCount !== importedIds.length) failures.push('Coverage counts are stale');
if (failures.length) {
  for (const failure of failures) console.error(failure);
  process.exitCode = 1;
} else {
  console.log(`Spec example coverage: ${sourceIds.length} classified, ${importedIds.length} imported, ${sourceIds.length - importedIds.length} explicit gaps/exclusions`);
}
