import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import Ajv from 'ajv';
import addFormats from 'ajv-formats';

const dataAjv = new Ajv({ allErrors: true, strict: false, validateFormats: false });
export function dataSchemaValidator(schema) {
  const valid = dataAjv.validateSchema(schema);
  dataSchemaValidator.errors = dataAjv.errors;
  return valid;
}

export function validators(root) {
  const ajv = new Ajv({ allErrors: true, strict: false, $data: true, validateFormats: false });
  addFormats(ajv);
  const names = readdirSync(join(root, 'schemas')).filter((name) => name.endsWith('.json'));
  const documents = names.map((name) => JSON.parse(readFileSync(join(root, 'schemas', name), 'utf8')));
  for (const schema of documents) {
    if (!ajv.validateSchema(schema)) throw new Error(`${schema.$id}: ${ajv.errorsText()}`);
    ajv.addSchema(schema);
  }
  return Object.fromEntries(names.map((name, index) => [name, ajv.getSchema(documents[index].$id)]));
}

export function replacePointer(value, pointer, replacement) {
  const segments = pointer.split('/').slice(1).map((part) => part.replaceAll('~1', '/').replaceAll('~0', '~'));
  const key = segments.pop();
  const parent = segments.reduce((item, part) => item[part], value);
  if (replacement === undefined) delete parent[key];
  else parent[key] = replacement;
}
