import { subjectTeachingContent, subjectArrangementContent } from './setup-subject-teaching.js';
import { renderAudienceQuality } from './audience-quality.js';
import { coordinationContent, createCoordinationActions } from './setup-coordination.js';
import { schoolDocument, parseSchoolDocument, applySchoolDocument, locatedDraftIssues, schoolDocumentSchema } from './school-document.js';
import { sections, entityKeys, emptyDraft, sourceDraft, readableDraft, copy, identity } from './setup-model.js';
import { storageKey, cleanupEpochKey } from './timetable-storage.js';
import { gridContent, createGridActions } from './setup-grid.js';
import { catalogDetail, createCatalogActions } from './setup-catalog.js';
import { plansContent, createPlanActions, configurePlanLimits, protectedReasons } from './setup-plans.js';
import { createCurriculumActions } from './setup-curriculum.js';
import { availabilityContent, createAvailabilityActions } from './setup-availability.js';
import { lunchContent, createLunchActions } from './setup-lunch.js';
import { createRoomActions, configureRoomLimits } from './setup-rooms.js';
import { createSchoolSave } from './setup-save.js';
import { mapDefinition, createExchange, downloadDocument } from './setup-exchange.js';
import { reviewContent, createReadiness } from './setup-review.js';
import { demoRequest } from './demo-transport.js';
import { createBlockActions } from './setup-blocks.js';
import { createSplitActions, splitsPreview } from './setup-splits.js';
import { relationshipsContent, createRelationshipActions } from './setup-relationships.js';
import { audiencePlansContent, createAudiencePlanActions } from './setup-audience-plans.js';
import { createAudienceTeacherActions } from './setup-audience-teachers.js';
const html = value => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);

export function createSetupEditor({ repository, school, storage, entry, qualityInfo = () => null, build = () => null, knownSchools = () => [], onSaved = () => {}, generationInfo = () => null, onGenerate = () => {}, onCancel = () => {}, onCleanup = () => {} }) {
  const qualityTitle = 'Student gaps and teacher workload';
  let qualityOpen = false;
  let budgets={quickSeconds:5,thoroughSeconds:60,lessonThreshold:200};
  let draft, timer, durability = 'Pending', message = '', storageError = '', recovery, disposed = false, contextSchoolId, panelMotion, jsonFeedback = '';
  const dialog = document.createElement('dialog');
  dialog.id = 'school-setup'; dialog.className = 'school-setup';
  dialog.setAttribute('aria-labelledby', 'setup-title');
  document.body.append(dialog);
  const grid = createGridActions({ getDraft: () => draft, html,
    commit(next, render = true) { draft = next; changed({ render }); },
    contextOnly(next) { flush(); draft = next; changed({ content: false, render: true }); flush(); },
    notify(text) { message = text; status(); }
  });
  const catalog = createCatalogActions({ getDraft: () => draft, getKey: () => entityKeys[draft.section], html,
    commit(next, render = true) { draft = next; changed({ render }); },
    contextOnly(next) { flush(); draft = next; changed({ content: false, render: true }); flush(); },
    notify(text) { message = text; status(); }, confirm: confirmScope
  });
  const splits = createSplitActions({getDraft:()=>draft,html,
    commit(next,render=true){draft=next;changed({render});},
    notify(text){message=text;status();},confirm:confirmScope});
  const relationships = createRelationshipActions({getDraft:()=>draft,html,confirm:confirmScope,
    commit(next,render=true){draft=next;changed({render});},
    contextOnly(next){flush();draft=next;changed({content:false,render:true});flush();}});
  const audiencePlans = createAudiencePlanActions({getDraft:()=>draft,html,confirm:confirmScope,
    notify(text){message=text;status();},commit(next,render=true){draft=next;changed({render});},
    contextOnly(next){flush();draft=next;changed({content:false,render:true});flush();}});
  const coordination = createCoordinationActions({getDraft:()=>draft,html,confirm:confirmScope,notify(text){message=text;status();},
    commit(next,render=true){draft=next;changed({render});},contextOnly(next){flush();draft=next;changed({content:false,render:true});flush();}});
  const audienceTeachers = createAudienceTeacherActions({getDraft:()=>draft,html,confirm:confirmScope,notify(text){message=text;status();},
    commit(next,render=true){draft=next;changed({render});}});
  const plans = createPlanActions({ getDraft: () => draft, html,
    commit(next, render = true) { draft = next; changed({ render }); },
    contextOnly(next, render = true) { flush(); draft = next; changed({ content: false, render }); },
    notify(text) { message = text; status(); }, confirm: confirmScope
  });
  const curriculum = createCurriculumActions({ getDraft: () => draft,
    commit(next) { draft=next; changed({render:true}); },
    contextOnly(next,render=true) { flush(); draft=next; changed({content:false,render}); },
    notify(text) { message=text;status(); }, confirm:confirmScope
  });
  const availability = createAvailabilityActions({ getDraft:()=>draft,
    commit(next) { draft=next; changed({render:true}); },
    contextOnly(next,render=true) { flush();draft=next;changed({content:false,render}); },
    notify(text) {message=text;status();}
  });
  const lunch = createLunchActions({ getDraft:()=>draft,
    commit(next) {draft=next;changed({render:true});},
    contextOnly(next) {flush();draft=next;changed({content:false,render:true});},
    notify(text) {message=text;status();}
  });
  const rooms = createRoomActions({getDraft:()=>draft,protectedReasons,
    commit(next,render=true){draft=next;changed({render});},notify(text){message=text;status();}
  });
  const blocks = createBlockActions({getDraft:()=>draft,protectedReasons,html,allIssues:locatedDraftIssues,
    commit(next,render=true){draft=next;changed({render});},notify(text){message=text;status();},confirm:confirmScope
  });
  const readiness = createReadiness({getDraft:()=>draft,build,request:demoRequest,render:renderDraft,flush,isDisposed:()=>disposed});
  const schoolSave=createSchoolSave({getDraft:()=>draft,repository,readiness,knownSchools,build,isDisposed:()=>disposed,cancelAutosave(){clearTimeout(timer);},notify(text){message=text;status();},onSaved(value){clearTimeout(timer);draft=null;dialog.close();onSaved(value);entry.focus();}});
  const exchange=createExchange({getDraft:()=>draft,activate,flush,notify(text){message=text;status();},confirm:confirmScope,request:demoRequest,build,isDisposed:()=>disposed});
  const all = () => Object.values(repository.state().setupDrafts ?? {}).filter(readableDraft);
  const rawNotice = () => Object.values(repository.state().setupDrafts ?? {}).some(d => !readableDraft(d))
    ? 'Some setup drafts have unreadable or unsupported authoring data. Their raw values are retained. Choose readable work, start empty, or use Clear local demo data in Advanced.' : '';
  function status() {
    const validation = dialog.querySelector('#setup-validation');if(validation)validation.textContent=draft?.validation??'';
    const saveState=dialog.querySelector('.setup-save-state');
    if(saveState)saveState.dataset.state=durability;
    const node = dialog.querySelector('#setup-durability');
    if (node) node.textContent = durability;
    const notice = dialog.querySelector('#setup-message');
    if (notice) notice.textContent = storageError || message || (draft?.advanced?.dirty ? 'Unapplied JSON changes. Open Advanced to apply or discard them before editing forms.' : rawNotice());
    const retry = dialog.querySelector('#setup-retry');
    if (retry) retry.hidden = !['Storage conflict', 'Only on this page'].includes(durability);
    const recover = dialog.querySelector('#setup-recover');
    if (recover) recover.hidden = durability !== 'Storage conflict';
  }
  function flush() {
    clearTimeout(timer);
    if (!draft || disposed) return true;
    durability = 'Saving'; status();
    try {
      repository.write(state => {
        state.setupDrafts ??= {}; state.setupDrafts[draft.id] = copy(draft);
        state.setupSelection ??= {}; state.setupSelection[contextSchoolId ?? school().id] = draft.id;
      });
      durability = 'Saved in this browser'; storageError = '';
      status(); return true;
    } catch (error) {
      durability = error.message.startsWith('Another tab') ? 'Storage conflict' : 'Only on this page';
      storageError = `${error.message} Keep this page open. Retry saving when storage is available, or download recovery to keep the full working snapshot.`;
      status(); return false;
    }
  }
  function changed({ content = true, render = false } = {}) {
    if (content) { message = ''; draft.revision++; draft.validation = draft.retained ? 'Unchecked' : 'Incomplete'; delete draft.check; delete draft.checkIssues; delete draft.checkNotice; }
    durability = 'Pending'; status(); clearTimeout(timer); timer = setTimeout(flush, 250);
    const issues = locatedDraftIssues(draft);
    for (const button of dialog.querySelectorAll('[data-section]')) {
      const count = button.querySelector('[aria-label="issues"]');
      if (count) { const n=issues.filter(i => i.section === button.dataset.section).length; count.textContent=n; count.hidden=!n; }
    }
    if (content && !draft.retained && !draft.advanced?.dirty) { const input=dialog.querySelector('#school-definition-json'); if(input)input.value=JSON.stringify(schoolDocument(draft),null,2); }
    if (render) renderDraft();
  }
  function activate(value, fresh = false) {
    dialog.querySelector("#setup-tools")?.removeAttribute("open");
    draft = value; qualityOpen = false;
    // Old matching checks need current build verification in UC-14, never trust a restored label alone.
    jsonFeedback = ''; message = ''; storageError = '';
    if (draft.validation === 'Ready') { draft.validation = 'Unchecked'; delete draft.check; delete draft.checkIssues; delete draft.checkNotice; }
    durability = fresh ? 'Pending' : 'Saved in this browser';
    renderDraft(); if (fresh) flush(); dialog.querySelector('#setup-name').focus();
  }
  function startScreen() {
    qualityOpen = false; draft = null;
    dialog.innerHTML = `<header class="setup-header"><h1 id="setup-title">School setup</h1><button data-close class="secondary">← Timetable</button></header>
      <nav aria-label="Setup sections"><button data-setup-start aria-current="page">School setup</button><button data-setup-quality>Student gaps and teacher workload</button></nav><section class="setup-start"><h2>Set up your school</h2><p>Edit an independent copy of the selected school, or start with an empty school.</p>
      <div class="setup-start-choices"><button id="setup-copy-source"><strong>Edit a copy</strong><span>${html(school().name)}</span><small>Original school and timetable preserved</small></button>
      <button id="setup-new"><strong>Create new school</strong><span>Start with an empty school</span><small>Build your own week, classes and teaching plans</small></button></div><p>Work is saved only in this browser on this device.</p></section>`;
  }
  function choose(sourceId) {
    const candidates = all().filter(d => d.source?.id === sourceId);
    const last = all().find(d => d.id === repository.state().setupSelection?.[sourceId]);
    if (last) { activate(last); return; }
    if (!candidates.length) { startScreen(); return; }
    if (candidates.length === 1) { activate(candidates[0]); return; }
    draft = null;
    dialog.innerHTML = `<header><h1 id="setup-title">Choose unfinished setup</h1><button data-close>Close setup</button></header>
      <p>Several drafts belong to this school. Select the work to resume.</p>${candidates.map(d => `<button class="setup-draft-choice" data-draft="${html(d.id)}">${html(d.name || 'Untitled school')} · ${html(d.source?.name)} · ${d.recoveredFrom ? 'Recovered draft' : 'Draft'} ${html(d.id)}</button>`).join('')}`;
  }
  function qualityContent() {
    const sourceId = draft ? draft.source?.id : school().id;
    const info = qualityInfo(sourceId);
    if (!info?.model) return `<p>${html(info?.message ?? 'Select a timetable for this school to see student gaps and teacher workload.')}</p>`;
    if (info.model.definition.schemaVersion !== 2) return '<p>Student gap and workload details are unavailable for this timetable.</p>';
    return `<p>${html(info.schoolName)} · ${html(info.timetableName)}. Based on the selected timetable; setup edits are separate.</p>${renderAudienceQuality(info.model, html, { embedded: true })}`;
  }
  function renderQuality() {
    qualityOpen = true;
    if (draft) return renderDraft();
    dialog.innerHTML = `<header class="setup-header"><h1 id="setup-title">School setup</h1><button data-close class="secondary">← Timetable</button></header>
      <div class="setup-layout"><aside class="setup-sidebar"><nav aria-label="Setup sections"><button data-setup-start>School setup</button><button data-setup-quality aria-current="page">${qualityTitle}</button></nav></aside>
      <div class="setup-work-area"><section id="setup-content" aria-labelledby="setup-section-title"><h2 id="setup-section-title" tabindex="-1">${qualityTitle}</h2>${qualityContent()}</section></div></div>`;
    dialog.querySelector('#setup-section-title').focus({ preventScroll: true });
  }
  function renderDraft() {
    const toolsOpen=dialog.querySelector('#setup-tools')?.open ?? false;
    const openGridDetails=[...dialog.querySelectorAll('#setup-content details[open]')].map(n=>n.id).filter(Boolean);
    const scrollTop=dialog.querySelector('#setup-content')?.scrollTop ?? 0;
    const openPlanSettings=new Set([...dialog.querySelectorAll('[data-plan] > .setup-row-settings[open]')].map(n=>n.closest('[data-plan]').dataset.plan));
    const issues = [...locatedDraftIssues(draft),...(draft.checkIssues??[])], source = draft.source;
    const activeSection = qualityOpen ? qualityTitle : draft.section;
    const descriptions = {
      [qualityTitle]: 'Inspect the student gaps and teacher workload of the selected timetable.',
      'Timetable grid': 'Arrange the school week',
      Subjects: 'Add the subjects taught at your school. Extra scheduling rules are optional.',
      Classes: 'Set up each class, then add its weekly subjects and teachers.',
      Teachers: 'Choose what each teacher can teach, for which classes, and when.',
      Rooms: 'Add your teaching spaces and how many students they can hold.',
      Review: 'Resolve remaining issues, check the school and save a named copy.'
    };
    dialog.innerHTML = `<header class="setup-header"><h1 id="setup-title">School setup</h1>
      <label class="setup-school-name"><span class="sr-only">School name</span><input id="setup-name" name="school-name" placeholder="Name your school…" value="${html(draft.name)}" autocomplete="off"></label>
      <span class="setup-save-state"><span class="setup-save-dot" aria-hidden="true"></span><strong id="setup-durability" role="status">${durability}</strong><span id="setup-validation" class="sr-only">${html(draft.validation)}</span></span>
      <button data-close class="secondary" aria-label="Close setup">← Timetable</button></header>
      <div class="setup-layout"><aside class="setup-sidebar"><nav aria-label="Setup sections">${sections.map((section,index) => {
        const count=issues.filter(i=>i.section===section).length;
        return `<button data-section="${section}" ${!qualityOpen && draft.section === section ? 'aria-current="page"' : ''}><span class="setup-step-number" aria-hidden="true">${index+1} ·</span><span>${section}</span><span class="setup-issue-count" aria-label="issues" ${count?'':'hidden'}>${count}</span></button>`;
      }).join('')}
      <button data-setup-quality ${qualityOpen?'aria-current="page"':''}>${qualityTitle}</button></nav><div class="setup-school-actions"><button id="setup-new" class="secondary">+ New school</button><button id="setup-copy-source" class="secondary">Copy selected school</button></div></aside>
      <div class="setup-work-area"><div class="setup-notice"><p id="setup-message" role="status"></p><div class="setup-actions"><button id="setup-retry" class="secondary" hidden>Retry saving draft</button><button id="setup-recover" class="secondary" hidden>Recover storage conflict…</button></div></div>
      <section id="setup-content" aria-labelledby="setup-section-title"><div class="setup-section-heading"><h2 id="setup-section-title" tabindex="-1">${activeSection}</h2><p class="setup-section-intro">${descriptions[activeSection]}</p></div><details id="setup-tools" class="setup-tools" ${qualityOpen?'hidden':''} ${toolsOpen?'open':''}><summary>Advanced</summary><div class="setup-tools-panel">${advancedContent()}
      <p id="setup-source">${source ? `Source: ${html(source.name)}. Uses the saved school definition; timetable overrides are separate.` : 'New school · independent unfinished draft'}${draft.recoveredFrom ? ` · Recovered from ${html(draft.recoveredFrom)}` : ''}</p>
      <label>Unfinished setup<select id="setup-draft-selector"><option value="${html(draft.id)}">${html(draft.name || 'Untitled school')} · Current draft</option>${all().filter(d => d.id !== draft.id).map(d => `<option value="${html(d.id)}">${html(d.name || 'Untitled school')} · ${html(d.source?.name || 'New school')} · ${d.recoveredFrom ? 'Recovered' : 'Draft'}</option>`).join('')}</select></label>
      <div class="setup-actions">
      <label>Import school JSON or recovery<input id="setup-import-file" type="file" accept=".json,application/json"></label><button id="setup-export" class="secondary">Export school JSON</button><button id="setup-schema" class="secondary">Download JSON Schema</button><button id="setup-download" class="secondary">Download recovery</button><button id="setup-discard" class="secondary">Discard this draft…</button></div>
      <div class="setup-local-data"><h3>Local demo data</h3><p>Clear local schools, timetables, candidates and unfinished work in this browser. Bundled schools remain available. This cannot be undone.</p><button id="delete-local-data" class="secondary">Clear local demo data…</button></div><small>Draft ${html(draft.id)} · Stored only in this browser on this device.</small></div></details>${sectionContent()}</section></div></div>
      <footer class="setup-footer"><span>Working draft · ${source?'Original school preserved':'Saved only in this browser'}</span><div class="setup-actions"><button data-back-timetable class="secondary">Close setup</button></div></footer>`;
    status();refreshGeneration();lockForms();
    for(const id of openGridDetails){const node=dialog.querySelector(`#${id}`);if(node)node.open=true;}
    for(const node of dialog.querySelectorAll('.setup-source-preview input, .setup-source-preview select, .setup-source-preview button:not([data-grid-day]):not([data-overview-day]), .setup-source-preview summary')) { if(node.tagName==='SUMMARY')continue;node.disabled=true; }
    dialog.querySelector("#setup-content").scrollTop=scrollTop;
    for(const node of dialog.querySelectorAll('[data-plan] > .setup-row-settings'))if(openPlanSettings.has(node.closest('[data-plan]').dataset.plan))node.open=true;
    if (dialog.open && !dialog.contains(document.activeElement)) dialog.querySelector('#setup-section-title').focus({ preventScroll: true });
  }
  function sectionContent() {
    if (qualityOpen) return qualityContent();
    if (draft.retained) {
      let preview='';
      if(draft.section==='Timetable grid') {
        try {const mapped=mapDefinition(draft.retained,draft.source).draft;mapped.pendingInput=draft.pendingInput;preview=`<div class="setup-source-preview">${gridContent(mapped,html)}</div>`;} catch {preview='<p>This source cannot be shown as a structured grid. Its original definition is retained.</p>';}
      }
      if(draft.section==='Review')return '<p>Save a new copy to use setup edits.</p>'+generationContent();
      const outdated=draft.source?.id===school().id && JSON.stringify(draft.retained)!==JSON.stringify(school().definition);
      return `<div class="setup-source-notice">${outdated ? '<p>The selected school has changed since this preview was saved. Your existing draft is preserved.</p><button data-current-source class="secondary">Open current school copy</button>' : ''}<p>Preview of ${html(draft.source?.name)}. Review the conversion to edit your own copy.</p><button id="setup-convert" class="secondary">Edit this copy…</button></div>${preview}${catalogContent()}`;
    }
    if (entityKeys[draft.section]) return catalogContent();
    if (draft.section === 'Review') return reviewContent(draft,html)+'<button id="setup-save-school">Save school copy</button><p>A fresh check saves a new immutable browser-local school; no timetable is generated.</p>'+generationContent();
    return gridContent(draft, html);
  }
  function generationContent(){
    const source=knownSchools().find(s=>s.id===draft.source?.id&&!s.bundled);
    if(!source)return '<p>Save a new school copy before generating. Bundled and unsaved setup do not start a solve.</p>';
    const effort=source.effort??(source.definition.lessons.length>budgets.lessonThreshold?'THOROUGH':'QUICK');
    return `<fieldset><legend>Generate for saved school: ${html(source.name)}</legend><p>Uses the immutable saved definition, including its original grid. This successor setup draft is independent; save another school copy to use its edits. Search failure is not proof of impossibility.</p><label>Search effort<select id="setup-effort"><option value="QUICK" ${effort==='QUICK'?'selected':''}>Quick (${budgets.quickSeconds}s)</option><option value="THOROUGH" ${effort==='THOROUGH'?'selected':''}>Thorough (${budgets.thoroughSeconds}s)</option></select></label><p>Budgets cover construction and search. Queueing, startup and checking add time. More time guarantees nothing.</p><button id="setup-start-generation">Generate saved school timetable</button><button id="setup-cancel-generation" class="secondary">Cancel generation</button><p id="setup-generation-status" role="status"></p></fieldset>`;
  }
  function refreshGeneration(){
    const node=dialog.querySelector('#setup-generation-status');if(!node)return;
    const info=generationInfo(draft?.source?.id),running=knownSchools().find(s=>['PREPARING','QUEUED','RUNNING','CHECKING_RESULT'].includes(s.generation?.status));
    node.textContent=running&&running.id!==draft?.source?.id?`Generation is active for ${running.name}. Use the timetable progress indicator to view it.`:info?.message??'Ready for an explicit generation request.';
    const active=['PREPARING','QUEUED','RUNNING','CHECKING_RESULT'].includes(info?.status);
    dialog.querySelector('#setup-start-generation').disabled=!!running||info?.status==='CANDIDATE';
    dialog.querySelector('#setup-cancel-generation').hidden=!active;
    dialog.querySelector('#setup-effort').disabled=!!running;
  }
  function catalogContent() {
    const key = entityKeys[draft.section]; if (!key) return '';
    const query = draft.searches?.[key] ?? '', focused = draft.focused?.[key];
    const records = draft[key].filter(r => `${r.name ?? r.displayName ?? ''} ${r.id}`.toLowerCase().includes(query.toLowerCase()));
    const selected = draft[key].find(r => r.id === focused);
    if(!draft.retained && key==='subjects' && selected?.id===draft.pendingInput?.subjectTeachingSubjectId) {
      const c=draft.classes.find(c=>c.id===draft.pendingInput.subjectTeachingClassId);
      if(c) {
        if(draft.pendingInput.subjectArrangement)return subjectArrangementContent(draft,c,selected,html);
        if(draft.plans.some(p=>(p.classId??p.cohortId)===c.id&&p.subjectId===selected.id))return plansContent(draft,c.id,html,{subjectId:selected.id});
        return audiencePlansContent(draft,c,html,{subjectId:selected.id});
      }
    }
    if(!draft.retained && key==='classes' && selected && selected.id===draft.pendingInput?.coordinationClassId)return coordinationContent(draft,selected,html);
    if(!draft.retained && key==='classes' && selected && selected.id===draft.pendingInput?.audiencePlanClassId)return audiencePlansContent(draft,selected,html);
    if(!draft.retained && key==='classes' && selected && selected.id===draft.pendingInput?.relationshipClassId)return relationshipsContent(draft,selected,html);
    if(!draft.retained && key==='classes' && selected && selected.id===draft.pendingInput?.planClassId)return plansContent(draft,selected.id,html);
    if(!draft.retained && selected && draft.pendingInput?.availability?.kind===key && draft.pendingInput.availability.id===selected.id)return availabilityContent(draft,key,selected,html);
    if(!draft.retained && selected && key==='classes' && draft.pendingInput?.lunch?.classId===selected.id)return lunchContent(draft,selected,html);
    return `<div class="setup-catalog-toolbar">${!draft.retained && ['subjects','classes','teachers','rooms'].includes(key) ? `<button id="setup-add-record">Add ${({subjects:'subject',classes:'class',teachers:'teacher',rooms:'room'})[key]}</button>` : ''}<label>Find ${draft.section.toLowerCase()}<input id="setup-search" value="${html(query)}" autocomplete="off"></label>
      <p id="setup-record-count">${records.length} of ${draft[key].length} records</p></div><div class="setup-record-browser"><div class="setup-record-list">${records.map(r => `<button data-record="${html(r.id)}" ${r.id === focused ? 'aria-current="true"' : ''}>${html(r.name ?? r.displayName ?? '(unnamed)')}${key==='classes' && r.splits?.length ? ` <small>· ${r.splits.reduce((n,s)=>n+s.groups.length,0)} groups</small>` : ''} </button>`).join('') || '<p>No matching records.</p>'}</div>
      <div id="setup-record-detail">${!draft.retained && ['subjects','classes','teachers','rooms'].includes(key) ? catalogDetail(draft, key, selected, html) : selected ? `<h3>${html(selected.name ?? selected.displayName)}</h3>${key==='classes' ? `${splitsPreview(draft,selected,html)}<details><summary>Advanced class data</summary><pre>${html(JSON.stringify(selected, null, 2))}</pre></details>` : key==='subjects' ? `${subjectTeachingContent(draft,selected,html)}<details><summary>Advanced subject data</summary><pre>${html(JSON.stringify(selected,null,2))}</pre></details>` : `<pre>${html(JSON.stringify(selected, null, 2))}</pre>`}` : '<p>Select a record to inspect its retained values.</p>'}</div></div>`;
  }
  function advancedContent() {
    let text = draft.advanced?.text ?? '';
    if (!draft.retained && !draft.advanced) text = JSON.stringify(schoolDocument(draft), null, 2);
    return `<div class="setup-json-editor"><h3>Advanced JSON</h3>
      <p>${draft.retained ? 'Review and accept the source conversion to edit its complete school document.' : 'Edit the same school as the forms. Apply changes before returning to form editing. Incomplete work can remain a draft.'}</p>
      ${!draft.retained ? `<label for="school-definition-json">Complete school JSON</label><textarea id="school-definition-json" spellcheck="false" autocomplete="off">${html(text)}</textarea>
      <div class="setup-actions"><button id="setup-apply-json">Apply to setup</button><button id="setup-discard-json" class="secondary" ${draft.advanced?.dirty?'':'disabled'}>Discard JSON changes…</button><button id="setup-pasted-import" class="secondary" ${draft.advanced?.dirty?'':'disabled'}>Import pasted legacy JSON…</button></div>` : ''}
      <p id="setup-json-status" role="status">${html(jsonFeedback || (draft.advanced?.dirty ? 'Unapplied JSON changes. Apply or discard them before editing forms.' : 'Forms and JSON use the same school document.'))}</p>
      ${repository.state().editor?.text ? `<details><summary>Previous JSON editor text</summary><p>Unfinished text from the previous editor is retained. ${html(repository.state().editor.name??'')}</p><label>Previous JSON buffer<textarea id="setup-previous-json" readonly>${html(repository.state().editor.text)}</textarea></label><div class="setup-actions"><button id="setup-legacy-buffer" class="secondary">Import previous JSON buffer…</button><button id="setup-download-legacy" class="secondary">Download previous JSON text</button></div></details>` : ''}</div>`;
  }
  function lockForms() {
    const pending = !!draft?.advanced?.dirty;
    for (const node of dialog.querySelectorAll('#setup-name, #setup-content input, #setup-content select, #setup-content textarea')) {
      if (node.closest('#setup-tools') || node.id==='setup-effort') continue;
      if (pending && !node.disabled) { node.dataset.jsonLocked = 'true'; node.disabled = true; }
      else if (!pending && node.dataset.jsonLocked) { node.disabled = false; delete node.dataset.jsonLocked; }
    }
    for (const id of ['setup-discard-json','setup-pasted-import']) { const button = dialog.querySelector('#'+id); if(button)button.disabled = !pending; }
    if (pending && dialog.querySelector('#setup-json-status') && !jsonFeedback) dialog.querySelector('#setup-json-status').textContent = 'Unapplied JSON changes. Apply or discard them before editing forms.';
  }
  function applyJson() {
    try {
      const text = draft.advanced?.text ?? dialog.querySelector('#school-definition-json').value;
      const document = parseSchoolDocument(text);
      const next = applySchoolDocument(draft, document);
      const issues = locatedDraftIssues(next);
      draft = next;
      jsonFeedback = issues.length ? 'Applied as draft. Resolve before saving:\n'+issues.map(i=>`${i.path}: ${i.message}`).join('\n') : 'Applied to setup. Check and save from Review.';
      changed({render:true}); flush(); return true;
    } catch(error) {
      jsonFeedback = error.message;
      dialog.querySelector('#setup-json-status').textContent = jsonFeedback; return false;
    }
  }
  function resolveJson() {
    const box = document.createElement('dialog'); box.className='setup-confirm';box.setAttribute('aria-label','Resolve unapplied JSON');
    box.innerHTML='<h2>Unapplied JSON changes</h2><p>Apply or discard your JSON changes before editing forms, checking or saving the school.</p><div class="setup-actions"><button data-keep autofocus>Keep editing JSON</button><button data-json-apply>Apply JSON</button><button data-json-discard class="secondary">Discard JSON changes…</button></div>';
    document.body.append(box);box.showModal();box.querySelector('[data-keep]').onclick=()=>{box.close();dialog.querySelector('#setup-tools').open=true;dialog.querySelector('#school-definition-json')?.focus();};
    box.querySelector('[data-json-apply]').onclick=()=>{box.close();dialog.querySelector('#setup-tools').open=true;applyJson();};
    box.querySelector('[data-json-discard]').onclick=()=>{box.close();dialog.querySelector('#setup-discard-json').click();};
    box.onclose=()=>box.remove();
  }
  dialog.addEventListener('click', event => {
    if (!draft?.advanced?.dirty) return;
    const button = event.target.closest('button');
    if (!button || ['setup-start-generation','setup-cancel-generation'].includes(button.id) || button.closest('#setup-tools') || button.dataset.section || button.hasAttribute('data-setup-quality') || button.hasAttribute('data-setup-start') || button.hasAttribute('data-quality-class') || button.hasAttribute('data-quality-teacher') || button.hasAttribute('data-close') || button.hasAttribute('data-back-timetable')) return;
    event.preventDefault(); event.stopImmediatePropagation(); resolveJson();
  }, true);
  function download(snapshot = draft) {
    try {
      const blob = new Blob([JSON.stringify({ documentType: 'school-setup-recovery', formatVersion: 1, draft: snapshot }, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob), link = document.createElement('a');
      link.href = url; link.download = `school-setup-${snapshot.id}.json`; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000); return true;
    } catch { message = 'Recovery download failed. Keep this page open; your working snapshot remains here.'; status(); return false; }
  }
  function confirmScope(title, text, action, label) {
    const box = document.createElement('dialog'); box.className = 'setup-confirm'; box.setAttribute('aria-label', title);
    box.innerHTML = `<h2>${html(title)}</h2><p>${html(text)}</p><div class="setup-actions"><button data-cancel autofocus>Cancel</button><button data-confirm>${html(label)}</button></div>`;
    document.body.append(box); box.showModal();
    box.querySelector('[data-cancel]').onclick = () => box.close();
    box.querySelector('[data-confirm]').onclick = () => { box.close(); action(); };
    box.onclose = () => box.remove();
  }
  function animatePanel(opening) {
    panelMotion?.cancel();
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return Promise.resolve();
    panelMotion=dialog.animate([{transform:opening?'translateX(-100%)':'translateX(0)'},{transform:opening?'translateX(0)':'translateX(-100%)'}], {duration:350,easing:'cubic-bezier(.22,1,.36,1)',fill:'both'});
    return panelMotion.finished.catch(()=>{});
  }
  async function close({ immediate = false } = {}) {
    if (!dialog.open) return;
    flush();
    for (const box of document.querySelectorAll('dialog.setup-confirm[open]')) box.close();
    dialog.dataset.closing='true';
    if (immediate) panelMotion?.cancel();
    else await animatePanel(false);
    if(dialog.dataset.closing!=='true')return;
    dialog.close();delete dialog.dataset.closing;panelMotion?.cancel();entry.focus();
  }
  dialog.addEventListener('cancel', event => { event.preventDefault(); close(); });
  dialog.addEventListener('input', event => {
    if (!draft) return;
    if (event.target.id === 'school-definition-json') { jsonFeedback = ''; draft.advanced = { text: event.target.value, dirty: true }; changed(); lockForms(); return; }
    if (!draft.retained && relationships.input(event)) return;
    if (!draft.retained && coordination.input(event)) return;
    if (!draft.retained && audiencePlans.input(event)) return;
    if (!draft.retained && audienceTeachers.input(event)) return;
    if (!draft.retained && event.target.type!=='checkbox' && splits.input(event)) return;
    if (!draft.retained && event.target.tagName!=='SELECT' && blocks.input(event)) return;
    if (!draft.retained && rooms.input(event)) return;
    if (!draft.retained && availability.input(event)) return;
    if (!draft.retained && plans.input(event)) return;
    if (!draft.retained && draft.section === 'Timetable grid' && grid.input(event)) return;
    if (!draft.retained && entityKeys[draft.section] && event.target.tagName !== 'SELECT' && catalog.input(event)) return;
    if (event.target.id === 'setup-name') { draft.name = event.target.value; changed(); }
    if (event.target.id === 'setup-search') {
      draft.searches ??= {}; draft.searches[entityKeys[draft.section]] = event.target.value;
      const start = event.target.selectionStart; changed({ content: false, render: true });
      const input = dialog.querySelector('#setup-search'); input.focus(); input.setSelectionRange(start, start);
    }
  });
  dialog.addEventListener('change', event => {
    if(draft&&!draft.retained&&relationships.change(event))return;
    if(draft&&!draft.retained&&coordination.change(event))return;
    if(draft&&!draft.retained&&audiencePlans.change(event))return;
    if(draft&&!draft.retained&&audienceTeachers.change(event))return;
    if(draft&&!draft.retained&&splits.change(event))return;
    if(event.target.id==='setup-import-file'){const file=event.target.files[0];event.target.value='';if(file)file.text().then(text=>exchange.preview(text,{id:'file',name:file.name,revision:null}));return;}
    if(draft&&!draft.retained&&event.target.tagName==='SELECT'&&blocks.change(event))return;
    if(draft&&!draft.retained&&rooms.change(event))return;
    if(draft&&!draft.retained&&availability.change(event))return;
    if(draft&&!draft.retained&&curriculum.change(event))return;
    if(draft&&!draft.retained&&plans.change(event))return;
    if (draft && !draft.retained && draft.section === 'Timetable grid' && grid.change(event)) return;
    if (draft && !draft.retained && entityKeys[draft.section] && event.target.tagName === 'SELECT' && catalog.change(event)) return;
    if (event.target.id === 'setup-draft-selector') { flush(); activate(all().find(d => d.id === event.target.value)); }
  });
  dialog.addEventListener('click', event => {
    const button = event.target.closest('button'); if (!button) return;
    if (button.hasAttribute('data-close') || button.hasAttribute('data-back-timetable')) return close();
    if (button.dataset.draft) return activate(all().find(d => d.id === button.dataset.draft));
    if (button.id === 'setup-new') { flush(); activate(emptyDraft(), true); return; }
    if (button.id === 'setup-copy-source' || button.hasAttribute('data-current-source')) { flush(); activate(sourceDraft(school()), true); return; }
    if (button.hasAttribute('data-setup-quality')) { flush(); renderQuality(); return; }
    if (button.hasAttribute('data-setup-start')) { qualityOpen = false; if(draft)renderDraft();else startScreen(); return; }
    if (!draft) return;
    if(draft.retained && (button.dataset.gridDay||button.dataset.overviewDay)){draft.pendingInput.gridDay=button.dataset.gridDay||button.dataset.overviewDay;changed({content:false,render:true});return;}
    if (button.id === 'setup-apply-json') return applyJson();
    if (button.id === 'setup-discard-json') return confirmScope('Discard JSON changes?', 'Your unapplied text will be discarded. The setup fields remain unchanged.', () => { delete draft.advanced; jsonFeedback = ''; changed({content:false,render:true}); flush(); }, 'Discard JSON changes');
    if (button.id === 'setup-schema') return downloadDocument(JSON.stringify(schoolDocumentSchema,null,2),'school-document-v1.schema.json');
    if (button.id === 'setup-pasted-import') return exchange.preview(draft.advanced?.text ?? '', {id:'pasted-json',name:'Pasted legacy JSON',revision:null});
    if (button.id === 'setup-download-legacy') return downloadDocument(repository.state().editor.text,'previous-school-definition.json');
    if (button.id === 'setup-legacy-buffer') return exchange.preview(repository.state().editor.text, {id:'advanced-buffer',name:'Previous JSON editor buffer',revision:null});
    if (button.id === 'delete-local-data') return onCleanup();
    if(exchange.click(button))return;
    if (!draft.retained && (button.hasAttribute('data-subject-teaching') || button.hasAttribute('data-add-subject-teaching'))) {
      const adding=button.hasAttribute('data-add-subject-teaching');
      const subjectId=adding?button.dataset.addSubjectTeaching:button.dataset.teachingSubject;
      const classId=adding?dialog.querySelector('#setup-subject-class')?.value:button.dataset.subjectTeaching;
      if(!draft.subjects.some(s=>s.id===subjectId)||!draft.classes.some(c=>c.id===classId))return;
      flush();
      if(adding) {
        if((draft.audiencePlans??[]).some(p=>p.classId===classId&&p.subjectId===subjectId)||draft.plans.some(p=>(p.classId??p.cohortId)===classId&&p.subjectId===subjectId))return;
        if(!draft.audiencePlans?.length)for(const c of draft.classes)if(!Object.hasOwn(c,'maxDailyGaps')&&draft.plans.some(p=>(p.classId??p.cohortId)===c.id))c.maxDailyGaps=0;
        (draft.audiencePlans??=[]).push({id:identity(),classId,subjectId,weeklyTotal:'',delivery:'whole',wholeClassCount:0,overrides:{},portions:[]});
      }
      draft.pendingInput.subjectTeachingSubjectId=subjectId;draft.pendingInput.subjectTeachingClassId=classId;delete draft.pendingInput.subjectArrangement;
      changed({content:adding,render:true});flush();dialog.querySelector('#setup-content').scrollTop=0;return;
    }
    if(!draft.retained && (button.hasAttribute('data-back-subject-teaching')||button.hasAttribute('data-back-subject-arrangement')||button.hasAttribute('data-manage-subject-groups'))) {
      flush();
      if(button.hasAttribute('data-back-subject-teaching')) {delete draft.pendingInput.subjectTeachingSubjectId;delete draft.pendingInput.subjectTeachingClassId;}
      if(button.hasAttribute('data-manage-subject-groups'))draft.pendingInput.subjectArrangement=true;else delete draft.pendingInput.subjectArrangement;
      changed({content:false,render:true});flush();dialog.querySelector('#setup-content').scrollTop=0;return;
    }
    if (!draft.retained && relationships.click(event)) return;
    if (!draft.retained && coordination.click(event)) return;
    if (!draft.retained && audiencePlans.click(event)) return;
    if (!draft.retained && audienceTeachers.click(event)) return;
    if (!draft.retained && splits.click(event)) return;
    if (!draft.retained && blocks.click(event)) return;
    if (!draft.retained && lunch.click(event)) return;
    if (!draft.retained && availability.click(event)) return;
    if (!draft.retained && curriculum.click(event)) return;
    if (!draft.retained && plans.click(event)) return;
    if (!draft.retained && draft.section === 'Timetable grid' && grid.click(event)) return;
    if (!draft.retained && entityKeys[draft.section] && catalog.click(event)) return;
    const section = button.dataset.section || button.dataset.goSection || button.dataset.issueSection;
    if (section) { flush(); qualityOpen = false; draft.section = section; changed({ content: false, render: true }); flush(); dialog.querySelector('#setup-content').scrollTop=0; dialog.querySelector('#setup-section-title').focus({preventScroll:true}); }
    if (button.dataset.record) { draft.focused ??= {}; draft.focused[entityKeys[draft.section]] = button.dataset.record; changed({ content: false, render: true }); dialog.querySelector('[aria-current="true"]')?.focus(); }
    if(button.id==='setup-check')readiness.check();
    if(button.id==='setup-save-school')schoolSave.save();
    if(button.id==='setup-start-generation')onGenerate(draft.source.id,dialog.querySelector('#setup-effort').value);
    if(button.id==='setup-cancel-generation')onCancel();
    if(button.hasAttribute('data-review-issue')){const issue=[...locatedDraftIssues(draft),...(draft.checkIssues??[])][Number(button.dataset.reviewIssue)];if(issue){draft.section=issue.section;if(issue.record&&entityKeys[issue.section])draft.focused[entityKeys[issue.section]]=issue.record;if(issue.split){delete draft.pendingInput.audiencePlanClassId;delete draft.pendingInput.coordinationClassId;delete draft.pendingInput.relationshipClassId;}if(issue.plan)draft.pendingInput.planClassId=issue.record;if(issue.audiencePlan)draft.pendingInput.audiencePlanClassId=issue.record;if(issue.coordination)draft.pendingInput.coordinationClassId=issue.record;if(issue.relationship){delete draft.pendingInput.audiencePlanClassId;delete draft.pendingInput.coordinationClassId;draft.pendingInput.relationshipClassId=issue.record;}if(issue.section==='Timetable grid'&&issue.record){const interval=draft.grid.intervals.find(i=>i.id===issue.record);if(interval)draft.pendingInput.gridDay=interval.day;}changed({content:false,render:true});if(issue.section==='Timetable grid'&&issue.record)dialog.querySelector(`[data-interval="${CSS.escape(issue.record)}"] input`)?.focus();}}
    if (button.id === 'setup-retry') flush();
    if (button.id === 'setup-download') { flush(); download(); }
    if (button.id === 'setup-discard') confirmScope('Discard this setup draft?', `Only draft ${draft.id} (${draft.name || 'Untitled school'}) will be removed. Saved schools, timetables and other drafts are kept.`, () => {
      clearTimeout(timer);
      try { repository.write(state => { delete state.setupDrafts?.[draft.id]; }, { atomic: true }); draft = null; dialog.close(); entry.focus(); }
      catch (error) { message = error.message; status(); }
    }, 'Discard draft');
    if (button.id === 'setup-recover') confirmScope('Recover a storage conflict', 'First download your complete current work. Then read the latest browser data. You can keep the recovered snapshot as a separate draft; edits are never merged.', () => {
      clearTimeout(timer); recovery = copy(draft);
      if (!download(recovery)) return;
      try {
        repository.reload();
        const latest = all().find(d => d.id === recovery.id);
        if (latest) draft = latest;
        else draft = sourceDraft(school());
        durability = latest ? 'Saved in this browser' : 'Pending';
        storageError = '';
        message = 'Latest browser data loaded. Your recovery snapshot is preserved on this page and in the download. Choose Keep separate recovered draft.';
        renderDraft(); status();
        const keep = document.createElement('button'); keep.id = 'setup-keep-recovered'; keep.textContent = 'Keep separate recovered draft';
        keep.onclick = () => { const recovered = copy(recovery); recovered.id = identity(); recovered.recoveredFrom = recovery.id; recovered.revision = 0; activate(recovered, true); };
        dialog.querySelector('.setup-notice .setup-actions').append(keep);
      } catch (error) { message = `Latest data could not be read: ${error.message}. Recovery remains on this page.`; status(); }
    }, 'Download recovery and read latest');
  });
  window.addEventListener('pagehide', flush);
  document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'hidden') flush(); });
  window.addEventListener('storage', event => {
    if ([storageKey,cleanupEpochKey].includes(event.key) && draft) { clearTimeout(timer); durability = 'Storage conflict'; message = 'Another tab changed browser data. This page’s work is retained. Recover before overwriting.'; status(); }
  });
  return {
    refreshQuality() { if (dialog.open && qualityOpen) renderQuality(); },
    refreshGeneration,
    close,
    configure(value) { budgets={...budgets,...value};configurePlanLimits(value); configureRoomLimits(value); },
    open({ review = false } = {}) {
      if (window.matchMedia('(max-width: 700px)').matches) return;
      const previousContext = contextSchoolId;
      contextSchoolId = school().id;
      if (draft && previousContext === school().id) renderDraft();
      else choose(school().id);
      if(review) { if(!draft){draft=sourceDraft(school());draft.section='Review';activate(draft,true);}else{draft.section='Review';renderDraft();} }
      delete dialog.dataset.closing;
      if(!dialog.open)dialog.showModal();
      animatePanel(true);
      (dialog.querySelector('#setup-section-title') ?? dialog.querySelector('button'))?.focus({preventScroll:true});
    },
    stop() { readiness.cancel(); disposed = true; clearTimeout(timer); dialog.close(); draft = null; }
  };
}
