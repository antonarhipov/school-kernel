async page => {
  const dir = 'output/playwright/school-definition-authoring/UC-21/ux-20261012-1';
  const errors = []; page.on('pageerror', error => errors.push(error.message));
  const read = async () => (await page.request.get('http://127.0.0.1:18112/api/workspace')).json();
  const canon = x => Array.isArray(x) ? x.map(canon) : x && typeof x === 'object' ? Object.fromEntries(Object.keys(x).sort().map(k => [k, canon(x[k])])) : x;
  const eq = (a,b,label) => { if (JSON.stringify(canon(a)) !== JSON.stringify(canon(b))) throw Error(label); };
  const check = (value,label) => { if (!value) throw Error(label); };
  const nav = async section => page.locator(`button[data-section="${section}"]`).click();
  await page.setViewportSize({width:1600,height:900}); await page.locator('#discard-authoring').waitFor(); const before = await read();
  await nav('Subjects'); await page.locator('#subjects-search').fill('AJALUGU');
  check(await page.locator('[data-subject-id="history-extra-a"]').count() === 1 && await page.locator('[data-subject-id="history-extra-b"]').count() === 1, 'duplicate identities');
  await page.locator('[data-subject-id="history-extra-a"]').click(); await page.locator('#subject-displayName').fill('Working <literal> Õ');
  await page.locator('#subjects-search').fill('no matching record'); check(await page.locator('#subject-displayName').inputValue() === 'Working <literal> Õ','filter lost working edit');
  check((await page.locator('.definition-browser-count').innerText()).includes('outside'),'hidden selection indication');
  await page.locator('#subjects-search').fill('history-extra'); await page.locator('[data-subject-id="history-extra-b"]').click(); await page.locator('#school-stay').click();
  check(await page.locator('#subject-id').inputValue() === 'history-extra-a','Stay changed selection'); eq(await read(),before,'browsing mutated workspace');
  await page.screenshot({path:dir+'/01-duplicate-names-unsaved.png'});
  await page.locator('[data-subject-id="history-extra-b"]').click(); await page.locator('#school-save-continue').click();
  await page.waitForFunction(() => document.querySelector('#subject-id')?.value === 'history-extra-b'); const saved = await read();
  check(saved.version === before.version + 1,'unexpected saves'); check(saved.workspace.definitionDraft.definition.subjects.find(x=>x.id==='history-extra-a').displayName === 'Working <literal> Õ','save exact');
  eq(saved.workspace.acceptedBaseline,before.workspace.acceptedBaseline,'Current changed'); eq(saved.workspace.definitionDraft.sourceDocument,before.workspace.definitionDraft.sourceDocument,'source changed');
  check(await page.locator('#subjects-search').inputValue() === 'history-extra','save lost search');
  await page.locator('#subjects-search').fill('OAS SELECTION'); check(await page.locator('[data-subject-id="oas-search-proof"]').count() === 1,'diacritic search');
  await page.locator('#subjects-search').press('ArrowDown'); await page.keyboard.press('Enter'); check(await page.locator('#subject-id').inputValue() === 'oas-search-proof','keyboard selection');
  await page.locator('#subjects-search').fill(''); check(await page.locator('.definition-record-choice').count() <= 25,'unbounded rows');
  await page.locator('.definition-browser-pager').getByRole('button',{name:'Next',exact:true}).click(); check((await page.locator('.definition-browser-pager').innerText()).includes('Page 2'),'paging');
  await nav('Teachers'); await nav('Subjects'); check((await page.locator('.definition-browser-pager').innerText()).includes('Page 2'),'roundtrip lost page');
  const views = [];
  for (const [section,collection,prefix] of [['Teachers','teachers','teacher'],['Classes','cohorts','class'],['Rooms','rooms','room']]) {
    await nav(section); const row = saved.workspace.definitionDraft.definition[collection][0];
    await page.locator(`#${collection}-search`).fill(row.id); await page.locator(`[data-${prefix}-id="${row.id}"]`).click();
    check(await page.locator(`#${prefix}-id`).inputValue() === row.id,'wrong record '+section);
    for (const width of [1600,1280]) { await page.setViewportSize({width,height:900}); check(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'overflow '+section+width); await page.screenshot({path:dir+`/${section.toLowerCase()}-${width}.png`}); }
    await page.locator(`#${prefix}-displayName`).fill('Retained desktop work');
    await page.setViewportSize({width:390,height:844}); await page.waitForFunction(()=>!document.querySelector('.definition-entity-form'));
    check(await page.locator(`#add-${prefix}`).count()===0,'narrow add'); check(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'narrow overflow');
    await page.screenshot({path:dir+`/${section.toLowerCase()}-390.png`});
    await page.locator(`#${collection}-search`).fill('no matches'); await page.setViewportSize({width:1280,height:800}); await page.locator(`#${prefix}-displayName`).waitFor();
    check(await page.locator(`#${prefix}-displayName`).inputValue()==='Retained desktop work','resize lost work');
    await page.locator(`#cancel-${prefix}`).click(); eq(await read(),saved,'resize/cancel mutation'); views.push(section);
  }
  await page.reload(); await page.locator('#discard-authoring').waitFor(); eq(await read(),saved,'reload mutation');
  check(errors.length===0,'page errors '+errors.join(';')); return {verdict:'PASS',beforeVersion:before.version,saved,views,errors};
}
