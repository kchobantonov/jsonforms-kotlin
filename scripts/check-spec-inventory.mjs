import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { assertPinnedSpecPackage } from './spec-package-source.mjs';

const read = (path) => readFileSync(resolve(path), 'utf8');
const json = (path) => JSON.parse(read(path));
const inventory = json('docs/spec-source-inventory.json');
const root = process.env.JSONFORMS_SPEC_ROOT ?? inventory.sourceRoot;
assertPinnedSpecPackage();
const source = (path) => json(`${root}/${path}`);
const catalog = source('examples/catalog.json');
const authoring = source('conformance/authoring.json');
const behavior = source('conformance/behavior.json');
const config = source('conformance/config-consumption.json');
const schemaFiles = [
  'jsonforms-extended-uischema.schema.json',
  'jsonforms-extended-config.schema.json',
  'jsonforms-rule.schema.json',
  'jsonforms-uischema.schema.json',
  'jsonforms-config.schema.json',
];
const schemaProperties = [];
function collectProperties(value, pointer, schema) {
  if (value === null || typeof value !== 'object') return;
  if (value.properties && typeof value.properties === 'object') {
    for (const key of Object.keys(value.properties)) {
      schemaProperties.push({ schema, pointer: `${pointer}/properties/${key}` });
    }
  }
  for (const [key, child] of Object.entries(value)) {
    collectProperties(child, `${pointer}/${key}`, schema);
  }
}
for (const schema of schemaFiles) collectProperties(source(`schemas/${schema}`), '', schema);
const sections = read(`${root}/docs/spec.md`).split('\n')
  .map((text, index) => ({ line: index + 1, text }))
  .filter(({ text }) => /^## (?:[1-9]|[12][0-9]|30)(?:\.|\s)/.test(text));
const expected = {
  sections,
  authoringVectors: authoring.map(({ id, section, schema, valid }) => ({ id, section: section ?? null, schema, valid: valid ?? null })),
  behaviorVectors: behavior.map(({ id, section }) => ({ id, section: section ?? null })),
  configPaths: config.properties.map(({ path, status }) => ({ path, referenceStatus: status })),
  schemaProperties,
  exampleIds: catalog.map(({ id }) => id),
};
const failures = [];
for (const [key, value] of Object.entries(expected)) {
  if (JSON.stringify(inventory[key]) !== JSON.stringify(value)) failures.push(`${key} changed: refresh and review the pinned inventory`);
}
const coverage = json('docs/spec-example-coverage.json');
if (inventory.specCommit !== coverage.specCommit) {
  failures.push('Inventory and example coverage source revisions differ');
}
if (JSON.stringify(coverage.examples.map(({ id }) => id).sort()) !== JSON.stringify(inventory.exampleIds.toSorted())) {
  failures.push('Example IDs are not fully classified in coverage');
}
for (const [name, values, key] of [
  ['authoring', inventory.authoringVectors, 'id'],
  ['behavior', inventory.behaviorVectors, 'id'],
  ['config', inventory.configPaths, 'path'],
  ['schema', inventory.schemaProperties, 'pointer'],
  ['example', inventory.exampleIds.map((id) => ({ id })), 'id'],
]) {
  const identifiers = values.map((value) => name === 'schema' ? `${value.schema}#${value.pointer}` : value[key]);
  if (new Set(identifiers).size !== identifiers.length) failures.push(`Duplicate ${name} identifiers`);
}
if (failures.length) {
  for (const failure of failures) console.error(failure);
  process.exitCode = 1;
} else {
  console.log(`Spec inventory: ${sections.length} sections, ${authoring.length} authoring, ${behavior.length} behavior, ${config.properties.length} config paths, ${schemaProperties.length} schema property occurrences, ${catalog.length} examples`);
}
