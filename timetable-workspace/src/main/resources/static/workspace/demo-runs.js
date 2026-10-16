export function effortControls(definition, config) {
  const large = definition.lessons.length > config.lessonThreshold;
  return `<section class="demo-effort"><label for="demo-effort">Search effort</label><select id="demo-effort">
    <option value="QUICK"${large ? '' : ' selected'}>Quick search (${config.quickSeconds} seconds)</option>
    <option value="THOROUGH"${large ? ' selected' : ''}>Thorough search (${config.thoroughSeconds} seconds)</option>
    </select><p class="muted">Budgets cover construction and search. Queueing, process startup and result checking take additional time. More time often helps but guarantees nothing.</p>
    <p id="demo-quick-warning" class="notice"${large ? '' : ' hidden'}>${large ? 'Quick search is usually too short for this school size.' : ''}</p></section>`;
}
export function bindEffort(definition, config) {
  const select = document.querySelector('#demo-effort');
  if (!select) return;
  const refresh = () => { document.querySelector('#demo-quick-warning').hidden = select.value !== 'QUICK' || definition.lessons.length <= config.lessonThreshold; };
  select.addEventListener('change', refresh); refresh();
}
export function runStatus(run) {
  const seconds = ms => Math.ceil(Math.max(0, Number(ms || 0)) / 1000);
  if (run.status === 'QUEUED') return `Queued · position ${run.queuePosition}. Approximate start in ${seconds(run.estimatedStartMs)} seconds; this is an estimate, not a promised start or finish time.`;
  if (run.status === 'CHECKING_RESULT') return 'Checking result · verifying the complete timetable before it becomes a proposal. Current is unchanged.';
  return `Running · ${seconds(run.elapsedTimeMs)} seconds since process dispatch. Search budget: ${run.limit}. Startup and checking are additional; Current is unchanged.`;
}
export function proposalProvenance(proposal) {
  if (!proposal.solveProvenance) return '';
  const seconds = ms => (Number(ms || 0) / 1000).toFixed(1);
  return `<p class="notice demo-provenance">${proposal.reused ? 'Result reused · no new computation or allowance reservation.' : 'Result computed and independently verified.'}
    Original solve process: ${seconds(proposal.originalSolveProcessMs)} seconds; original checking: ${seconds(proposal.originalVerificationProcessMs)} seconds.
    This request waited ${seconds(proposal.waitMs)} seconds. Search budget: ${proposal.limit}. This proposal is private; Current changes only after explicit acceptance.</p>`;
}
