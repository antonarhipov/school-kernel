// Anonymous class attendance. Coordinates are stable group identities, never labels.
export const intersectionLimit = 4096;
const positive = value => Number.isInteger(value) && value > 0;
const unionMembers = (r,id) => Object.hasOwn(r.unions??{},id) ? r.unions[id] : [];
export const coordinatesKey = ids => JSON.stringify([...ids].sort());
export function jointDimensions(splits) {
  if (splits.reduce((n,s)=>n*s.groups.length,1)>intersectionLimit) return null;
  return splits.reduce((rows,s)=>rows.flatMap(row=>s.groups.map(g=>[...row,g.id])),[[]]);
}
export function resolveRelationships(c, selectedIds = c.relationships?.splitIds ?? []) {
  const issues=[], blocks=[], r=c.relationships;
  const add=message=>issues.push({section:'Classes',record:c.id,relationship:true,message:`${c.displayName??c.id}: ${message}`});
  const splits=selectedIds.map(id=>c.splits?.find(s=>s.id===id));
  if(new Set(selectedIds).size!==selectedIds.length){add('Each relevant split must be selected once.');return {blocks,issues};}
  if(splits.some(s=>!s)){add('A selected split is missing. Correct its explicit reference.');return {blocks,issues};}
  if(!splits.length)return {blocks:[{id:`class:${c.id}`,size:c.size,groupIds:[]}],issues};
  for(const s of splits)if(!s.groups.length||s.groups.some(g=>!positive(g.size))||s.groups.reduce((n,g)=>n+g.size,0)!==c.size){add(`Correct group sizes in ${s.displayName||s.id} before resolving relationships.`);}
  if(issues.length)return {blocks,issues};
  const block=(groupIds,size)=>({id:`block:${c.id}:${coordinatesKey(groupIds)}`,groupIds,size});
  if(splits.length===1)return {blocks:splits[0].groups.map(g=>block([g.id],g.size)),issues};
  if(!r){add('Relationships are missing. Reuse one split, declare nested groups, or supply joint overlap counts.');return {blocks,issues};}
  if(r.mode==='nested'){
    const base=splits.find(s=>s.id===r.constituentSplitId);
    if(!base){add('Select the constituent split containing the smallest groups.');return {blocks,issues};}
    for(const g of base.groups){
      const ids=[g.id];
      for(const s of splits.filter(s=>s!==base)){
        const parents=s.groups.filter(parent=>unionMembers(r,parent.id).includes(g.id));
        if(parents.length!==1)add(`${g.displayName} must belong to exactly one group of ${s.displayName}; found ${parents.length}.`);
        else ids.push(parents[0].id);
      }
      blocks.push(block(ids,g.size));
    }
    for(const s of splits.filter(s=>s!==base))for(const g of s.groups){
      const members=unionMembers(r,g.id);
      if(new Set(members).size!==members.length)add(`${g.displayName} repeats a constituent audience.`);
      if(members.some(id=>!base.groups.some(g=>g.id===id)))add(`${g.displayName} has a missing constituent reference.`);
      const total=base.groups.filter(g=>members.includes(g.id)).reduce((n,g)=>n+g.size,0);
      if(total!==g.size)add(`${s.displayName} / ${g.displayName}: constituent total ${total}, declared size ${g.size}.`);
    }
  }else if(r.mode==='joint'){
    const dimensions=jointDimensions(splits);
    if(!dimensions){add(`Combined intersections exceed the disclosed ${intersectionLimit} row limit. Reuse or declare a nested arrangement.`);return {blocks,issues};}
    const expected=new Set(dimensions.map(coordinatesKey)),seen=new Set();
    for(const row of r.intersections??[]){
      const key=coordinatesKey(row.groupIds);
      if(seen.has(key))add(`Repeated joint intersection ${key}.`);seen.add(key);
      if(!expected.has(key))add(`Unresolved intersection coordinates ${key}; correct changed group references.`);
      if(!Number.isInteger(row.count)||row.count<0)add(`Intersection ${key}: supply a nonnegative integer count.`);
      else if(row.count>0&&expected.has(key))blocks.push(block(row.groupIds,row.count));
    }
    const missing=[...expected].filter(k=>!seen.has(k));
    if(missing.length)add(`${missing.length} combined intersections need explicit counts, including zero; pairwise margins do not establish a joint distribution.`);
    for(const s of splits)for(const g of s.groups){
      const total=blocks.filter(b=>b.groupIds.includes(g.id)).reduce((n,b)=>n+b.size,0);
      if(total!==g.size)add(`${s.displayName} / ${g.displayName}: joint margin ${total}, declared size ${g.size}.`);
    }
    if(blocks.reduce((n,b)=>n+b.size,0)!==c.size)add(`Joint total ${blocks.reduce((n,b)=>n+b.size,0)} must equal class total ${c.size}.`);
  }else add('Choose nested groups or advanced joint overlap counts.');
  return {blocks:issues.length?[]:blocks,issues};
}
const label=(c,id)=>c.splits?.flatMap(s=>s.groups.map(g=>({id:g.id,name:`${s.displayName} / ${g.displayName}`}))).find(g=>g.id===id)?.name??`Missing group ${id}`;
export function relationshipsContent(draft,c,html){
  const r=c.relationships??{splitIds:[],mode:'nested',unions:{},intersections:[]};
  const splits=r.splitIds.map(id=>c.splits?.find(s=>s.id===id)).filter(Boolean),base=splits.find(s=>s.id===r.constituentSplitId);
  const dimensions=splits.length>1?jointDimensions(splits):[];
  const validCoordinates=new Set((dimensions??[]).map(coordinatesKey));
  const retained=(r.intersections??[]).filter(row=>!validCoordinates.has(coordinatesKey(row.groupIds)));
  return `<div data-relationship-class="${html(c.id)}"><button id="setup-back-relationships" class="secondary">← Class details</button><h3>Split relationships · ${html(c.displayName)}</h3>
    <p>Reuse the same split across subjects when the audience is the same. Different names or equal counts do not establish relationships.</p>
    <fieldset><legend>Relevant splits</legend>${(c.splits??[]).map(s=>`<label><input type="checkbox" data-related-split="${html(s.id)}" ${r.splitIds.includes(s.id)?'checked':''}>${html(s.displayName||'Unnamed split')} · ${s.groups.length} groups</label>`).join('')}</fieldset>
    ${splits.length>1?`<label>Relationship type<select id="setup-relationship-mode"><option value="nested" ${r.mode==='nested'?'selected':''}>Nested groups</option><option value="joint" ${r.mode==='joint'?'selected':''}>Advanced joint overlap counts</option></select></label>
    ${r.mode==='nested'?`<p>Choose the smallest constituent arrangement, then select which constituent groups compose each larger group.</p><label>Constituent split<select id="setup-constituent-split"><option value="">Choose a split</option>${splits.map(s=>`<option value="${html(s.id)}" ${s.id===r.constituentSplitId?'selected':''}>${html(s.displayName)}</option>`).join('')}</select></label>
    ${base?splits.filter(s=>s!==base).map(s=>`<h4>${html(s.displayName)}</h4>${s.groups.map(g=>`<fieldset><legend>${html(g.displayName)} · ${g.size} students</legend>${base.groups.map(child=>`<label><input type="checkbox" data-union="${html(g.id)}" data-constituent="${html(child.id)}" ${unionMembers(r,g.id).includes(child.id)?'checked':''}>${html(child.displayName)} · ${child.size}</label>`).join('')}</fieldset>`).join('')}`).join(''):''}`:
    `<p>Supply every combined intersection, including zero. Three or more splits require joint information; pairwise totals are insufficient. Limit: ${intersectionLimit} combined intersections. Zero creates no empty block.</p>
    ${dimensions?dimensions.map(ids=>{const value=(r.intersections??[]).find(row=>coordinatesKey(row.groupIds)===coordinatesKey(ids))?.count??'';return `<label>${ids.map(id=>html(label(c,id))).join(' ∩ ')}<input data-intersection="${html(JSON.stringify(ids))}" value="${html(value)}" inputmode="numeric" aria-describedby="setup-relationship-issues"></label>`;}).join(''):`<p role="alert">Too many combined intersections. Reuse or declare nested groups.</p>`}`}`:'<p>Select at least two splits to declare their relationship. One split already partitions the class.</p>'}
    ${r.mode==='joint'&&retained.length?`<details><summary>Retained incompatible intersection rows (${retained.length})</summary><p>These earlier coordinates are preserved; they do not describe the currently selected combined dimensions.</p><ul>${retained.map(row=>`<li>${row.groupIds.map(id=>html(label(c,id))).join(' ∩ ')}: ${html(row.count)}</li>`).join('')}</ul><button id="setup-remove-incompatible-joints" class="secondary">Review removal of incompatible rows…</button></details>`:''}
    <div id="setup-relationship-result">${relationshipFeedback(c,html)}</div>
    <p>Draft retention is separate from readiness. Changes to groups may invalidate these declarations; supplied values remain recoverable.</p></div>`;
}
function relationshipFeedback(c,html){
  const {blocks,issues}=resolveRelationships(c);
  return `<div id="setup-relationship-issues" role="status">${issues.map(i=>`<p>${html(i.message)}</p>`).join('')||'<p>Selected audience relationships are consistent.</p>'}</div>
    ${blocks.length?`<h4>Derived anonymous attendance</h4><p>Total: ${blocks.reduce((n,b)=>n+b.size,0)} / ${html(c.size)}</p><ul>${blocks.map(b=>`<li>${b.groupIds.map(id=>html(label(c,id))).join(' ∩ ')||'Whole class'}: ${b.size}</li>`).join('')}</ul>`:''}`;
}
export function createRelationshipActions({getDraft,commit,contextOnly,html,confirm}){
  function edit(event){
    const root=event.target.closest('[data-relationship-class]');if(!root)return false;
    const draft=getDraft(),c=draft.classes.find(c=>c.id===root.dataset.relationshipClass),r=c.relationships??={splitIds:[],mode:'nested',unions:{},intersections:[]},node=event.target;
    let render=true;
    if(node.dataset.relatedSplit){r.splitIds=r.splitIds.filter(id=>id!==node.dataset.relatedSplit);if(node.checked)r.splitIds.push(node.dataset.relatedSplit);}
    else if(node.id==='setup-relationship-mode')r.mode=node.value;
    else if(node.id==='setup-constituent-split')r.constituentSplitId=node.value;
    else if(node.dataset.union){const list=unionMembers(r,node.dataset.union).filter(id=>id!==node.dataset.constituent);if(node.checked)list.push(node.dataset.constituent);r.unions={...r.unions,[node.dataset.union]:list};render=false;}
    else if(node.dataset.intersection){const ids=JSON.parse(node.dataset.intersection),key=coordinatesKey(ids);let row=r.intersections.find(row=>coordinatesKey(row.groupIds)===key);if(!row){row={groupIds:ids};r.intersections.push(row);}row.count=/^-?\d+$/.test(node.value)&&Number.isSafeInteger(Number(node.value))?Number(node.value):node.value;render=false;}
    else return false;
    commit(draft,render);if(!render)root.querySelector('#setup-relationship-result').innerHTML=relationshipFeedback(c,html);return true;
  }
  return {input:event=>event.target.dataset.intersection?edit(event):false,change:event=>event.target.dataset.intersection?false:edit(event),click(event){
    const button=event.target.closest('button');if(!button)return false;const draft=getDraft();
    if(button.id==='setup-remove-incompatible-joints'){
      const c=draft.classes.find(c=>c.id===button.closest('[data-relationship-class]').dataset.relationshipClass),r=c.relationships;
      const dimensions=jointDimensions(r.splitIds.map(id=>c.splits.find(s=>s.id===id)).filter(Boolean));
      if(!dimensions)return true;
      const keys=new Set(dimensions.map(coordinatesKey)),rows=r.intersections.filter(row=>!keys.has(coordinatesKey(row.groupIds)));
      confirm('Remove incompatible intersection rows?',rows.map(row=>`${row.groupIds.map(id=>label(c,id)).join(' ∩ ')}: ${row.count}`).join('; ')+'. Current combined counts and nested unions are preserved.',()=>{r.intersections=r.intersections.filter(row=>keys.has(coordinatesKey(row.groupIds)));commit(draft);},'Remove incompatible rows');return true;
    }
    if(button.dataset.openRelationships){draft.pendingInput.relationshipClassId=button.dataset.openRelationships;contextOnly(draft);return true;}
    if(button.id==='setup-back-relationships'){delete draft.pendingInput.relationshipClassId;contextOnly(draft);return true;}
    return false;
  }};
}
export function readableRelationships(c){
  const r=c.relationships;if(r===undefined)return true;
  return r&&['nested','joint'].includes(r.mode)&&Array.isArray(r.splitIds)&&r.splitIds.every(id=>typeof id==='string')
    &&(!Object.hasOwn(r,'constituentSplitId')||typeof r.constituentSplitId==='string')
    &&r.unions&&typeof r.unions==='object'&&!Array.isArray(r.unions)&&Object.values(r.unions).every(ids=>Array.isArray(ids)&&ids.every(id=>typeof id==='string'))
    &&Array.isArray(r.intersections)&&r.intersections.every(row=>row&&Array.isArray(row.groupIds)&&row.groupIds.every(id=>typeof id==='string')&&['number','string'].includes(typeof row.count)&&Object.keys(row).every(k=>['groupIds','count'].includes(k)))
    &&Object.keys(r).every(k=>['mode','splitIds','constituentSplitId','unions','intersections'].includes(k));
}
