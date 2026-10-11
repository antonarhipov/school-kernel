async page => {
 const read=async()=>await(await page.request.get('http://127.0.0.1:18107/api/workspace')).json();
 await page.reload();await page.locator('#confirm-repair-accept').waitFor();const before=await read();if(before.state!=='REPAIR_PROPOSAL'||before.version!==10)throw Error('proposal restart not restored');
 await page.screenshot({path:'output/playwright/school-definition-authoring/UC-17/20261012-3/15-restored-proposal.png'});
 await page.locator('#confirm-repair-accept').check();await page.locator('#accept-repair').click();await page.getByText(/Local storage is unavailable/).first().waitFor({timeout:30000});const after=await read();
 const canon=x=>Array.isArray(x)?x.map(canon):x&&typeof x==='object'?Object.fromEntries(Object.keys(x).sort().map(k=>[k,canon(x[k])])):x;
 if(JSON.stringify(canon(before))!==JSON.stringify(canon(after)))throw Error('failed accept mutated durable state');await page.screenshot({path:'output/playwright/school-definition-authoring/UC-17/20261012-3/16-accept-storage-failure.png'});return {verdict:'PASS',before,after,snapshot:await page.locator('body').ariaSnapshot()};
}
