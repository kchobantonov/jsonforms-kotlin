import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

const read = (path) => readFileSync(path, 'utf8');
const inventory = JSON.parse(read('docs/renderer-registry-inventory.json'));
const parse = (source) => [...source.matchAll(/JsonFormsRendererRegistryEntry\(\s*(?:tester\s*=\s*)?(\w+)\s*,\s*(?:renderer\s*=\s*)?(\w+)\s*,?\s*\)/g)]
  .map((match, order) => ({ order, tester: match[1], renderer: match[2] }));
const parseCells = (source) => [...source.matchAll(/JsonFormsCellRendererRegistryEntry\(\s*(\w+)\s*,\s*ComposeJsonFormsCellRenderer\s*\{\s*([\w.]+)\(/g)]
  .map((match, order) => ({ order, tester: match[1], cell: match[2].split('.').at(-1) }));
const failures = [];
for (const family of ['material', 'cupertino']) {
  const entry = inventory[family];
  const source = read(entry.source);
  if (JSON.stringify(entry.renderers) !== JSON.stringify(parse(source))) failures.push(`${family} renderer registry changed`);
  if (JSON.stringify(entry.cells) !== JSON.stringify(parseCells(source))) {
    failures.push(`${family} cell registry changed`);
  }
}
function filesUnder(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = join(directory, entry.name);
    return entry.isDirectory() ? filesUnder(path) : path.endsWith('.kt') ? [path] : [];
  });
}
const cupertinoSource = 'cupertino-renderers/src/commonMain/kotlin/com/chobantonov/jsonfroms/cupertino/renderers';
const delegates = filesUnder(cupertinoSource).flatMap((source) => {
  const match = read(source).match(/val\s+(\w+)\s*=\s*ComposeJsonFormsRenderer\s*\{\s*(Material\w+)\.Render\(it\)/);
  return match ? [{ cupertinoRenderer: match[1], materialRenderer: match[2], source }] : [];
});
const byName = (a, b) => a.cupertinoRenderer.localeCompare(b.cupertinoRenderer);
if (JSON.stringify(delegates.sort(byName)) !== JSON.stringify(inventory.cupertino.delegatesToMaterial.toSorted(byName))) {
  failures.push('Cupertino direct delegation inventory changed');
}
for (const delegate of delegates) {
  if (!inventory.cupertino.renderers.some(({ renderer }) => renderer === delegate.cupertinoRenderer)) {
    failures.push(`Unregistered Cupertino delegate: ${delegate.cupertinoRenderer}`);
  }
}
const extended = read(inventory.extended.source);
const extendedEntries = [...extended.matchAll(/JsonFormsRendererRegistryEntry\(\s*(\w+)\s*,\s*((?:controls|layouts)\.\w+|\w+Renderer(?:\(cupertino\))?)\s*(?:,\s*handlesLabelInterpolation\s*=\s*(true|false))?\s*\)/g)]
  .map((match) => ({ tester: match[1], implementation: match[2],
    ...(match[3] ? { handlesLabelInterpolation: match[3] === 'true' } : {}) }));
if (JSON.stringify(extendedEntries) !== JSON.stringify(inventory.extended.renderers)) failures.push('Extended renderer registry changed');
const extendedCells = inventory.extended.cells;
const cellCall = /ComposeJsonFormsCellRenderer \{ (\w+)\((it(?:\.asRendererProps\(\))?), cupertino = (true|false)(?:, compact = (true|false))?\) \}/g;
const materialCellSource = extended.split('val materialExtendedCells:')[1]?.split('val cupertinoExtendedCells:')[0] ?? '';
const cupertinoCellSource = extended.split('val cupertinoExtendedCells:')[1] ?? '';
const actualExtendedCells = [
  ...[...materialCellSource.matchAll(cellCall)].map((match) => ({ family: 'material', implementation: match[1], propsAdapter: match[2] === 'it' ? null : 'asRendererProps', cupertino: match[3] === 'true', compact: match[4] === 'true' })),
  ...[...cupertinoCellSource.matchAll(cellCall)].map((match) => ({ family: 'cupertino', implementation: match[1], propsAdapter: match[2] === 'it' ? null : 'asRendererProps', cupertino: match[3] === 'true', compact: match[4] === 'true' })),
];
const expectedExtendedCells = extendedCells.map(({ family, implementation, propsAdapter = null, cupertino, compact = false }) => ({ family, implementation, propsAdapter, cupertino, compact }));
if (JSON.stringify(actualExtendedCells) !== JSON.stringify(expectedExtendedCells)) failures.push('Extended cell registry changed');
for (const { family, tester, implementation, propsAdapter, cupertino, compact } of extendedCells) {
  const props = propsAdapter ? `it.${propsAdapter}()` : 'it';
  const compactArg = compact ? ', compact = true' : '';
  if (!extended.includes(`val ${family}ExtendedCells: List<JsonFormsCellRendererRegistryEntry<ComposeJsonFormsCellRenderer>>`) ||
      !extended.includes(`ComposeJsonFormsCellRenderer { ${implementation}(${props}, cupertino = ${cupertino}${compactArg}) }`) ||
      !extended.includes(cupertino ? `withIncreasedRank(100, ${tester})` : `JsonFormsCellRendererRegistryEntry(${tester},`)) {
    failures.push(`${family} extended cell registry changed`);
  }
}
if (failures.length) {
  for (const failure of failures) console.error(failure);
  process.exitCode = 1;
} else {
  console.log(`Renderer registry: ${inventory.material.renderers.length} Material, ${inventory.cupertino.renderers.length} Cupertino, ${inventory.extended.renderers.length} extended, ${delegates.length} Cupertino delegations; ${inventory.material.cells.length} Material, ${inventory.cupertino.cells.length} Cupertino and ${extendedCells.length} extended cells`);
}
