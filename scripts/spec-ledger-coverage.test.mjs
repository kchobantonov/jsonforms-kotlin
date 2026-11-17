import test from 'node:test';
import assert from 'node:assert/strict';
import { missingRequirementSections, misplacedRequirementIds } from './spec-ledger-coverage.mjs';

test('finds sections that lost every anchored requirement', () => {
  const sections = [
    { number: 1, auditStatus: 'partial' },
    { number: 2, auditStatus: 'not-audited' },
  ];
  assert.deepEqual(missingRequirementSections(sections, [{ id: 'S1-01' }]), [2]);
});

test('only declared experimental and summary sections can omit rows', () => {
  const sections = [
    { number: 11, auditStatus: 'experimental-out-of-scope' },
    { number: 26, auditStatus: 'summary-cross-reference' },
    { number: 27, auditStatus: 'partial' },
  ];
  assert.deepEqual(missingRequirementSections(sections, []), [27]);
  assert.deepEqual(missingRequirementSections([
    { number: 11, auditStatus: 'not-audited' },
    { number: 26, auditStatus: 'not-audited' },
  ], []), [11, 26]);
});

test('requires each requirement anchor to remain inside its numbered section', () => {
  const sections = [{ number: 1, line: 10 }, { number: 2, line: 20 }];
  const requirements = [
    { id: 'S0-01', sourceLine: 9 },
    { id: 'S1-01', sourceLine: 19 },
    { id: 'S2-01', sourceLine: 20 },
    { id: 'S1-02', sourceLine: 20 },
    { id: 'S2-02', sourceLine: 18 },
  ];
  assert.deepEqual(misplacedRequirementIds(sections, requirements), ['S1-02', 'S2-02']);
});
