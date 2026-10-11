const el=(tag,text)=>{const n=document.createElement(tag);if(text!==undefined)n.textContent=text;return n;};
let pending=false;
export const finishBusy=()=>pending;
export function renderFinish(host,snapshot,narrow,actions,leave){
 const button=el('button','Finish verified definition');button.id='finish-definition';button.disabled=narrow||pending;host.append(button);
 button.addEventListener('click',()=>leave(async latest=>{
  if(pending)return;pending=true;button.disabled=true;let dialog;
  const close=()=>{dialog?.close();dialog?.remove();pending=false;actions.render(latest);};
  try{
   const preview=await actions.entity('finish/preview',{},latest.version);
   dialog=el('dialog');dialog.id='finish-definition-dialog';dialog.append(el('h3','Finish this saved definition?'),el('p',`Saved workspace version ${preview.workspaceVersion} • ${preview.review.draftRevision}`),el('p',`${preview.review.counts.blockers} blocking issues; ${preview.review.counts.advisories} advisories. Destination: ${preview.destination}. This verifies the definition; it does not solve or publish a timetable.`));
   if(preview.review.zeroLessonExplanation)dialog.append(el('p',preview.review.zeroLessonExplanation));
   for(const issue of preview.review.issues)dialog.append(el('p',`${issue.severity}: ${issue.ownerType} ${issue.ownerId} • ${issue.field}: ${issue.message}`));
   dialog.append(el('p',`${preview.review.counts.changes} saved changes; inspect the saved Review before confirming.`));
   const confirm=el('button','Confirm finish and verify');confirm.id='confirm-definition-finish';confirm.disabled=preview.review.counts.blockers>0;
   const cancel=el('button','Cancel finish');cancel.addEventListener('click',close);
   const feedback=el('pre');feedback.setAttribute('role','alert');feedback.style.whiteSpace='pre-wrap';
   const refresh=el('button','Refresh saved review');refresh.hidden=true;refresh.addEventListener('click',async()=>{close();await actions.refresh();});
   confirm.addEventListener('click',async()=>{confirm.disabled=true;cancel.disabled=true;feedback.textContent='Verifying the complete saved definition…';try{const result=await actions.entity('finish',{token:preview.token,confirmed:true},latest.version);dialog.close();dialog.remove();pending=false;actions.render(result);}catch(error){feedback.textContent=`Finish was not committed. ${error.message}`;confirm.disabled=false;cancel.disabled=false;refresh.hidden=false;}});
   dialog.append(confirm,cancel,refresh,feedback);dialog.addEventListener('cancel',event=>{event.preventDefault();if(!cancel.disabled)close();});host.append(dialog);dialog.showModal();
  }catch(error){pending=false;const notice=el('p',`Finish review unavailable. ${error.message}`);notice.setAttribute('role','alert');host.append(notice);button.disabled=narrow;}
 }));
}
