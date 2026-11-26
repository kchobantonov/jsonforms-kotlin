import { readFileSync } from 'node:fs';
import { missingRequirementSections, misplacedRequirementIds } from './spec-ledger-coverage.mjs';
import { assertPinnedSpecPackage } from './spec-package-source.mjs';

const read = (path) => readFileSync(path, 'utf8');
const candidates = JSON.parse(read('docs/spec-normative-candidates.json'));
const inventory = JSON.parse(read('docs/spec-source-inventory.json'));
assertPinnedSpecPackage();
const specRoot = process.env.JSONFORMS_SPEC_ROOT ?? inventory.sourceRoot;
const lines = read(`${specRoot}/docs/spec.md`).split('\n');
const expected = [];
let buffer = [];
let start = 1;
let section = '';
let subsection = '';
for (let index = 0; index <= lines.length; index++) {
  const line = lines[index] ?? '';
  if (line.startsWith('## ')) section = line;
  if (line.startsWith('### ')) subsection = line;
  if (line.trim() === '') {
    if (buffer.length) {
      const text = buffer.join('\n');
      const keywords = [...text.matchAll(/\b(?:MUST(?: NOT)?|SHOULD(?: NOT)?|REQUIRED|RECOMMENDED|MAY)\b/g)].map((match) => match[0]);
      if (keywords.length) expected.push({ line: start, section, subsection, keywords, text });
      buffer = [];
    }
    start = index + 2;
  } else {
    if (!buffer.length) start = index + 1;
    buffer.push(line);
  }
}
const actual = candidates.candidates.map(({ line, section, subsection, keywords, text }) => ({ line, section, subsection, keywords, text }));
const ledger = JSON.parse(read('docs/spec-requirement-ledger.json'));
const sectionNumbers = inventory.sections.map(({ text }) => Number(text.match(/^## (\d+)/)?.[1]));
const ledgerSections = ledger.sections.map(({ number }) => number);
const mappedLines = new Set(ledger.requirements.map(({ sourceLine }) => sourceLine));
const unmappedNormative = candidates.candidates.filter(({ classification, line }) => classification === 'normative' && !mappedLines.has(line));
const duplicateIds = ledger.requirements.map(({ id }) => id).filter((id, index, ids) => ids.indexOf(id) !== index);
const missingSections = missingRequirementSections(ledger.sections, ledger.requirements);
const misplacedIds = misplacedRequirementIds(ledger.sections, ledger.requirements);
const sourceMapped = ({ sourceLine, sourceAnchor }) =>
  candidates.candidates.some(({ line }) => line === sourceLine) ||
  (typeof sourceAnchor === 'string' && sourceAnchor.length > 0 &&
    lines.slice(sourceLine - 1, sourceLine + 6).join('\n').includes(sourceAnchor));
if (ledger.specCommit !== inventory.specCommit || JSON.stringify(sectionNumbers) !== JSON.stringify(ledgerSections) ||
    unmappedNormative.length || duplicateIds.length || missingSections.length || misplacedIds.length ||
    ledger.requirements.some((requirement) => !sourceMapped(requirement))) {
  console.error(`Requirement ledger section coverage or normative candidate mapping changed${missingSections.length ? `: missing §§${missingSections.join(', ')}` : ''}${misplacedIds.length ? `: misplaced ${misplacedIds.join(', ')}` : ''}`);
  process.exitCode = 1;
}
if (candidates.specCommit !== inventory.specCommit || JSON.stringify(actual) !== JSON.stringify(expected)) {
  console.error('Normative candidate list or pinned revision changed; refresh and review it');
  process.exitCode = 1;
} else if (candidates.candidates.some(({ classification }) => !['needs-review', 'normative', 'proposal', 'definition', 'example'].includes(classification))) {
  console.error('Unknown candidate classification');
  process.exitCode = 1;
} else {
  const pending = candidates.candidates.filter(({ classification }) => classification === 'needs-review').length;
  console.log(`Normative candidates: ${expected.length} paragraphs, ${pending} awaiting manual classification`);
  if (pending > 0) process.exitCode = 1;
}
