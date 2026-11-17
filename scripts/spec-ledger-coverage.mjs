// §11 is explicitly experimental; §26 restates requirements from earlier sections.
// Every other numbered section must have at least one anchored ledger requirement.
const sectionExceptions = new Map([
  [11, 'experimental-out-of-scope'],
  [26, 'summary-cross-reference'],
]);

export function missingRequirementSections(sections, requirements) {
  const covered = new Set(requirements.map(({ id }) => Number(/^S(\d+)[.-]/.exec(id)?.[1])));
  return sections
    .filter(({ number, auditStatus }) =>
      !covered.has(number) && sectionExceptions.get(number) !== auditStatus)
    .map(({ number }) => number);
}

export function misplacedRequirementIds(sections, requirements) {
  const ordered = [...sections].sort((a, b) => a.line - b.line);
  return requirements
    .filter(({ id, sourceLine }) => {
      const number = Number(/^S(\d+)[.-]/.exec(id)?.[1]);
      if (number === 0) return sourceLine >= ordered[0].line;
      const index = ordered.findIndex((section) => section.number === number);
      return index < 0 || sourceLine < ordered[index].line ||
        (index + 1 < ordered.length && sourceLine >= ordered[index + 1].line);
    })
    .map(({ id }) => id);
}
