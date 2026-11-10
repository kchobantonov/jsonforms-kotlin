import { existsSync, mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { join, resolve } from 'node:path';

const root = resolve(process.env.JSONFORMS_SPEC_EXAMPLES_DIR ?? '../jsonforms-extended-spec/examples');
const destination = resolve('examples/spec');
const catalog = JSON.parse(readFileSync(join(root, 'catalog.json'), 'utf8'));
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
    .filter((child) => !(child?.type === 'Category' && child.label === 'AG Grid'))
    .map(withoutAgGridCategory);
  if (value === null || typeof value !== 'object') return value;
  return Object.fromEntries(Object.entries(value).map(([key, child]) =>
    [key, withoutAgGridCategory(child)]));
};
const omitted = [];
const fixtures = catalog.flatMap(({ id, title }) => {
  const fixture = { id, title };
  for (const key of ['data', 'schema', 'uischema', 'config', 'translations']) {
    const file = join(root, id, `${key}.json`);
    if (existsSync(file)) fixture[key] = JSON.parse(readFileSync(file, 'utf8'));
  }
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
  if (unsupportedUiSchema(fixture.uischema)) {
    omitted.push(id);
    return [];
  }
  return [fixture];
});
const commit = process.env.JSONFORMS_SPEC_COMMIT ??
  execFileSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' }).trim();
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
}
writeFileSync(join(destination, 'catalog.json'), `${JSON.stringify({ commit, examples: fixtures.map(({ id, title }) => ({ id, title })) }, null, 2)}\n`);
console.log(`Synchronized ${fixtures.length} spec examples`);
console.log(`Omitted browser-only fixtures: ${omitted.join(', ')}`);
