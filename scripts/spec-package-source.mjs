import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const readJson = (path) => JSON.parse(readFileSync(resolve(path), 'utf8'));
export const inventory = readJson('docs/spec-source-inventory.json');
export const specRoot = resolve(process.env.JSONFORMS_SPEC_ROOT ?? inventory.sourceRoot);

export function assertPinnedSpecPackage() {
  const installed = readJson(`${specRoot}/package.json`);
  const project = readJson('package.json');
  const lock = readJson('package-lock.json');
  const locked = lock.packages?.[`node_modules/${inventory.packageName}`];
  if (installed.name !== inventory.packageName || installed.version !== inventory.packageVersion ||
      project.devDependencies?.[inventory.packageName] !== inventory.packageVersion ||
      locked?.version !== inventory.packageVersion || locked.integrity !== inventory.packageIntegrity) {
    throw new Error(`Spec package must be ${inventory.packageName}@${inventory.packageVersion} with the pinned lockfile integrity`);
  }
  return installed;
}
