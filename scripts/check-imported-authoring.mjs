import { readdirSync, readFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const json = (path) => JSON.parse(readFileSync(resolve(path), 'utf8'));
const specRoot = resolve(process.env.JSONFORMS_SPEC_ROOT ?? json('docs/spec-source-inventory.json').sourceRoot);
const { validators, dataSchemaValidator, replacePointer } = await import(
  pathToFileURL(resolve(specRoot, 'scripts/validation.mjs')).href
);
const checks = await validators(specRoot);
const imported = json('examples/spec/catalog.json');
const pinnedCommit = json('docs/spec-source-inventory.json').specCommit;
const sourceCatalog = json(`${specRoot}/examples/catalog.json`);
const expectedInvalid = json(`${specRoot}/conformance/expected-invalid.json`);
const failures = [];
const sourceIds = new Set(sourceCatalog.map(({ id }) => id));
const importedIds = new Set(imported.examples.map(({ id }) => id));
const draft07MetaSchema = 'http://json-schema.org/draft-07/schema#';
const usesDraft07MetaSchema = (schema) =>
  schema.$schema === undefined || schema.$schema === draft07MetaSchema;
let documents = 0;
let draft07Schemas = 0;
let schemasWithoutDialect = 0;

if (imported.commit !== pinnedCommit ||
  execFileSync('git', ['rev-parse', 'HEAD'], { cwd: specRoot, encoding: 'utf8' }).trim() !== pinnedCommit) {
  failures.push('Imported catalog revision differs from the pinned spec inventory');
}
for (const entry of sourceCatalog) {
  if (!checks['example.schema.json'](entry)) failures.push(`source catalog ${entry.id}: invalid metadata`);
}
for (const expected of expectedInvalid) {
  if (!importedIds.has(expected.example)) continue;
  if (!readdirSync(resolve(`examples/spec/${expected.example}`)).includes(expected.file)) {
    failures.push(`${expected.example}/${expected.file}: declared authoring exception is missing`);
  }
}

const profile = {
  'schema.json': dataSchemaValidator,
  'uischema.json': checks['jsonforms-extended-uischema.schema.json'],
  'config.json': checks['jsonforms-extended-config.schema.json'],
  'dynamic-uischema.json': checks['jsonforms-dynamic-uischema.schema.json'],
};
for (const { id } of imported.examples) {
  if (!sourceIds.has(id)) failures.push(`${id}: not present in the pinned source catalog`);
  const directory = resolve(`examples/spec/${id}`);
  for (const file of readdirSync(directory)) {
    const path = resolve(directory, file);
    if (profile[file]) {
      const value = json(path);
      if (file === 'schema.json') {
        const dialect = value.$schema;
        if (!usesDraft07MetaSchema(value)) {
          failures.push(`${id}/${file}: unsupported data-schema dialect ${JSON.stringify(dialect)}; select its meta-schema validator`);
          continue;
        }
        if (dialect === undefined) schemasWithoutDialect++;
        else draft07Schemas++;
      }
      const original = JSON.stringify(value);
      const validate = profile[file];
      const exceptions = expectedInvalid.filter(item => item.example === id && item.file === file);
      if (exceptions.length) {
        if (validate(value)) failures.push(`${id}/${file}: expected authoring rejection was absent`);
        const errors = validate.errors ?? [];
        for (const item of exceptions) {
          if (!errors.some(error => error.instancePath === item.pointer ||
            error.instancePath.startsWith(`${item.pointer}/`))) {
            failures.push(`${id}/${file}${item.pointer}: expected rejection was absent`);
          }
        }
        const repaired = structuredClone(value);
        for (const item of exceptions) replacePointer(repaired, item.pointer, item.replacement);
        if (!validate(repaired)) failures.push(`${id}/${file}: invalid after declared repairs: ${JSON.stringify(validate.errors)}`);
      } else if (!validate(value)) {
        failures.push(`${id}/${file}: ${JSON.stringify(validate.errors)}`);
      }
      if (JSON.stringify(value) !== original) failures.push(`${id}/${file}: authoring validation mutated the fixture`);
      documents++;
    } else if (file === 'uischemas.json') {
      const entries = json(path);
      if (!Array.isArray(entries)) failures.push(`${id}/${file}: expected a registry array`);
      else for (const [index, entry] of entries.entries()) {
        const original = JSON.stringify(entry);
        if (!checks['jsonforms-extended-uischema.schema.json'](entry.uischema)) {
          failures.push(`${id}/${file}[${index}]: ${JSON.stringify(checks['jsonforms-extended-uischema.schema.json'].errors)}`);
        }
        if (entry.match !== undefined) {
          const match = entry.match;
          const allowed = new Set(['rank', 'schemaPath', 'notSchemaPath', 'schemaPointer', 'exists', 'equals']);
          if (!match || typeof match !== 'object' || Array.isArray(match) ||
            Object.keys(match).some(key => !allowed.has(key)) ||
            !Number.isFinite(match.rank) || match.rank < 0 ||
            typeof match.schemaPointer !== 'string' || !match.schemaPointer.startsWith('/') ||
            (match.schemaPath !== undefined && typeof match.schemaPath !== 'string') ||
            (match.notSchemaPath !== undefined && typeof match.notSchemaPath !== 'string') ||
            (match.exists !== undefined && match.exists !== true) ||
            (!Object.hasOwn(match, 'equals') && match.exists !== true)) {
            failures.push(`${id}/${file}[${index}]: invalid portable registry match`);
          }
        }
        if (JSON.stringify(entry) !== original) failures.push(`${id}/${file}[${index}]: authoring validation mutated the registry entry`);
        documents++;
      }
    } else if (file === 'translations.json') {
      const catalogs = json(path);
      if (JSON.stringify(Object.keys(catalogs).sort()) !== JSON.stringify(['bg', 'en']) ||
        !catalogs.en || !catalogs.bg ||
        JSON.stringify(Object.keys(catalogs.en).sort()) !== JSON.stringify(Object.keys(catalogs.bg).sort())) {
        failures.push(`${id}/${file}: require only English and Bulgarian catalogs with matching keys`);
      }
      documents++;
    }
  }
}

const control = { type: 'Control', scope: '#/properties/value', options: { focus: true } };
if (!checks['jsonforms-extended-uischema.schema.json']({ type: 'Control', scope: '#/properties/missing' })) {
  failures.push('Extended authoring profile rejects a structurally valid unresolved Control scope');
}
if (usesDraft07MetaSchema({ $schema: 'https://json-schema.org/draft/2020-12/schema' })) {
  failures.push('Draft-07 authoring gate silently accepts a different declared dialect');
}
if (dataSchemaValidator({ type: 'not-a-json-schema-type' })) {
  failures.push('Draft-07 meta-schema probe accepts an invalid schema type');
}
for (const name of ['jsonforms-uischema.schema.json', 'jsonforms-extended-uischema.schema.json']) {
  const validate = checks[name];
  if (!validate(control)) failures.push(`${name}: rejects a valid common Control`);
  if (validate({ ...control, options: { focus: 'true' } })) failures.push(`${name}: accepts a quoted Boolean focus option`);
  if (!validate({ ...control, options: { vendorOption: { enabled: true } } })) {
    failures.push(`${name}: rejects an unknown vendor option`);
  }
  if (!validate({ ...control, options: { layout: { span: 2, start: 3, responsive: { small: 1 } } } })) {
    failures.push(`${name}: rejects reserved child layout hints`);
  }
  const layoutWithoutDefaults = { type: 'HorizontalLayout', elements: [structuredClone(control)] };
  const authoredLayout = JSON.stringify(layoutWithoutDefaults);
  if (!validate(layoutWithoutDefaults)) failures.push(`${name}: rejects a valid layout without defaulted options`);
  if (JSON.stringify(layoutWithoutDefaults) !== authoredLayout) {
    failures.push(`${name}: inserted schema defaults into an authored layout`);
  }
}
const extendedUi = checks['jsonforms-extended-uischema.schema.json'];
for (const variant of ['chips', 'multi-select']) {
  if (!extendedUi({ ...control, options: { variant } })) {
    failures.push(`Extended static authoring profile rejects Control variant ${variant}`);
  }
}
const category = { type: 'Category', label: 'First', elements: [control] };
for (const variant of ['stepper', 'accordion']) {
  if (!extendedUi({ type: 'Categorization', options: { variant }, elements: [category] })) {
    failures.push(`Extended static authoring profile rejects Categorization variant ${variant}`);
  }
}
const dynamicVariant = { $dynamic: { path: '#/presentation' } };
if (extendedUi({ ...control, options: { variant: dynamicVariant } })) {
  failures.push('Extended static authoring profile accepts a dynamic Control variant descriptor');
}
if (extendedUi({ type: 'Categorization', options: { variant: dynamicVariant }, elements: [category] })) {
  failures.push('Extended static authoring profile accepts a dynamic Categorization variant descriptor');
}
if (!checks['jsonforms-extended-uischema.schema.json']({
  type: 'VendorWidget', options: { vendorOption: { enabled: true } },
})) failures.push('Extended UI profile rejects an unknown downstream element type');
if (!checks['jsonforms-extended-uischema.schema.json']({
  type: 'Button', label: 'Execute', script: 'console.log(1)',
})) failures.push('Extended UI profile rejects a structurally valid script Button');
const rule = { effect: 'SHOW', condition: { scope: '#/properties/visible', schema: { const: true } } };
if (!checks['jsonforms-rule.schema.json'](rule)) failures.push('Standalone rule profile rejects a valid rule');
if (checks['jsonforms-rule.schema.json']({ ...rule, effect: 'INVALID' })) {
  failures.push('Standalone rule profile accepts an invalid effect');
}
for (const name of ['jsonforms-config.schema.json', 'jsonforms-extended-config.schema.json']) {
  if (!checks[name]({ restrict: false })) failures.push(`${name}: rejects explicit false restrict`);
  const configWithoutDefaults = {};
  if (!checks[name](configWithoutDefaults)) failures.push(`${name}: rejects a config without defaulted fields`);
  if (Object.keys(configWithoutDefaults).length) failures.push(`${name}: inserted schema defaults into a config`);
}
const extendedConfig = checks['jsonforms-extended-config.schema.json'];
const security = { jsonformsExtended: { security: { urlPolicy: { allowImageDataUrls: true } } } };
if (!extendedConfig(security)) failures.push('Extended config profile rejects a valid URL security option');
if (extendedConfig({ jsonformsExtended: { security: { urlPolicy: { allowImageDataUrls: 'true' } } } })) {
  failures.push('Extended config profile accepts a quoted Boolean URL security option');
}

if (failures.length) {
  for (const failure of failures) console.error(failure);
  process.exitCode = 1;
} else {
  console.log(`Authoring schemas: ${Object.keys(checks).length} registered, ${sourceCatalog.length} source catalog entries, ${imported.examples.length} imports, ${documents} local documents/registries (${draft07Schemas} declared draft-07, ${schemasWithoutDialect} implicit draft-07 data schemas)`);
}
