import { existsSync, mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { join, resolve } from 'node:path';

const root = resolve(process.env.JSONFORMS_SPEC_EXAMPLES_DIR ?? '../jsonforms-extended-spec/examples');
const destination = resolve('examples/spec');
const catalog = JSON.parse(readFileSync(join(root, 'catalog.json'), 'utf8'));
const coverage = JSON.parse(readFileSync(resolve('docs/spec-example-coverage.json'), 'utf8'));
const adaptationRoot = resolve('examples/spec-adaptations');
const adaptationCatalog = JSON.parse(readFileSync(join(adaptationRoot, 'catalog.json'), 'utf8'));
const adaptations = adaptationCatalog.adaptations;
const unknownAdaptations = Object.keys(adaptations).filter((id) => !catalog.some((example) => example.id === id));
if (unknownAdaptations.length) throw new Error(`Adaptations absent from spec catalog: ${unknownAdaptations.join(', ')}`);
const classifiedIds = new Set(coverage.examples.map(({ id }) => id));
const unclassified = catalog.filter(({ id }) => !classifiedIds.has(id)).map(({ id }) => id);
if (unclassified.length) throw new Error(`Classify new spec examples before syncing: ${unclassified.join(', ')}`);
const materializeLabels = (value, english) => {
  if (Array.isArray(value)) return value.map((child) => materializeLabels(child, english));
  if (value === null || typeof value !== 'object') return value;
  const mapped = Object.fromEntries(Object.entries(value).map(([key, child]) =>
    [key, materializeLabels(child, english)]));
  if (mapped.type === 'Label' && typeof mapped.text !== 'string') {
    mapped.text = english[`${mapped.i18n}.text`] ?? english[mapped.i18n] ?? mapped.i18n ?? '';
  }
  return mapped;
};
const unsupportedUiSchema = (uischema) => {
  const source = JSON.stringify(uischema);
  return /ag.?grid|monaco|"type":"TemplateLayout"|"type":"Template"/i.test(source);
};
const withoutAgGridCategory = (value) => {
  if (Array.isArray(value)) return value
    .filter((child) => !(child?.type === 'Category' &&
      /^AG Grid(?:$|:)/.test(child.label ?? '')))
    .map(withoutAgGridCategory);
  if (value === null || typeof value !== 'object') return value;
  return Object.fromEntries(Object.entries(value).map(([key, child]) =>
    [key, withoutAgGridCategory(child)]));
};
const omitted = [];
const fixtures = catalog.flatMap(({ id, title }) => {
  const fixture = { id, title };
  for (const key of ['data', 'invalid-data', 'schema', 'uischema', 'uischemas', 'config', 'translations']) {
    const file = join(root, id, `${key}.json`);
    if (existsSync(file)) fixture[key] = JSON.parse(readFileSync(file, 'utf8'));
  }
  if (adaptations[id]) {
    fixture.title = adaptations[id].title;
    for (const key of ['data', 'invalid-data', 'schema', 'uischema', 'uischemas', 'config', 'translations']) {
      const file = join(adaptationRoot, id, `${key}.json`);
      if (existsSync(file)) fixture[key] = JSON.parse(readFileSync(file, 'utf8'));
    }
  }
  if (id === 'table-row-details') fixture.uischema = withoutAgGridCategory(fixture.uischema);
  if (id === 'file-control') {
    fixture.uischema = withoutAgGridCategory(fixture.uischema);
    if (fixture.translations) fixture.translations = Object.fromEntries(
      Object.entries(fixture.translations).map(([locale, entries]) =>
        [locale, {
          ...Object.fromEntries(Object.entries(entries).filter(([key]) => !/gridTab/i.test(key))),
          'tableIntro.text': locale === 'bg'
            ? 'Таблицата редактира документите. Свързаните файлове се показват като етикети с имената; добавете файлове, премахнете един или изчистете избора.'
            : 'The table edits the documents. Related files shows filename pills; add files, remove one, or clear the selection.',
        }]));
  }
  if (fixture.uischema) fixture.uischema = materializeLabels(fixture.uischema, fixture.translations?.en ?? {});
  if (unsupportedUiSchema(fixture.uischema) && !['template-slots', 'job-application'].includes(id)) {
    omitted.push(id);
    return [];
  }
  return [fixture];
});
const commit = process.env.JSONFORMS_SPEC_COMMIT ??
  execFileSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' }).trim();
if (adaptationCatalog.specCommit !== commit) throw new Error('Adaptation manifest targets a different spec revision');
const catalogText = `${JSON.stringify({ commit, examples: fixtures.map(({ id, title }) => ({ id, title })) }, null, 2)}\n`;
if (process.argv.includes('--check')) {
  const failures = [];
  const catalogFile = join(destination, 'catalog.json');
  if (!existsSync(catalogFile) || readFileSync(catalogFile, 'utf8') !== catalogText) failures.push('catalog.json');
  const selected = new Set(fixtures.map(({ id }) => id));
  for (const item of readdirSync(destination, { withFileTypes: true })) {
    if (item.isDirectory() && !selected.has(item.name)) failures.push(`obsolete directory ${item.name}`);
  }
  for (const { id, title, ...files } of fixtures) {
    for (const [key, value] of Object.entries(files)) {
      const file = join(destination, id, `${key}.json`);
      if (!existsSync(file) || readFileSync(file, 'utf8') !== `${JSON.stringify(value, null, 2)}\n`) failures.push(`${id}/${key}.json`);
    }
    const readme = join(adaptationRoot, id, 'README.md');
    if (existsSync(readme)) {
      const output = join(destination, id, 'README.md');
      if (!existsSync(output) || readFileSync(output, 'utf8') !== readFileSync(readme, 'utf8')) failures.push(`${id}/README.md`);
    }
  }
  if (failures.length) throw new Error(`Spec snapshot is out of sync: ${failures.join(', ')}`);
  console.log(`Spec snapshot matches ${fixtures.length} selected examples, including checked-in adaptations`);
  process.exit(0);
}
mkdirSync(destination, { recursive: true });
const selected = new Set(fixtures.map(({ id }) => id));
for (const item of readdirSync(destination, { withFileTypes: true })) {
  if (item.isDirectory() && !selected.has(item.name)) rmSync(join(destination, item.name), { recursive: true });
}
for (const { id, title, ...files } of fixtures) {
  const directory = join(destination, id);
  mkdirSync(directory, { recursive: true });
  for (const [key, value] of Object.entries(files)) {
    writeFileSync(join(directory, `${key}.json`), `${JSON.stringify(value, null, 2)}\n`);
  }
  const readme = join(adaptationRoot, id, 'README.md');
  if (existsSync(readme)) writeFileSync(join(directory, 'README.md'), readFileSync(readme, 'utf8'));
}
writeFileSync(join(destination, 'catalog.json'), catalogText);
console.log(`Synchronized ${fixtures.length} spec examples`);
console.log(`Omitted fixtures pending renderer support or platform adaptation: ${omitted.join(', ')}`);
