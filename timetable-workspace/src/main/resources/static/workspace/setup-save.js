const normalized=value=>value.trim().toLocaleLowerCase();
export function createSchoolSave({getDraft,repository,readiness,knownSchools,build,onSaved,notify,isDisposed,cancelAutosave}){
 let saving=false;
 async function save(){
  if(saving||isDisposed())return;
  const draft=getDraft(),id=draft.id,revision=draft.revision,name=draft.name.trim();
  const nameError=()=>!name?'Enter a school name.':name.length>120?'Use at most 120 characters for the school name.':knownSchools().some(s=>normalized(s.name)===normalized(name))?'A school with this name already exists. Choose another name.':null;
  const issue=nameError();if(issue){notify(issue);return;}
  saving=true;
  try{
   const check=await readiness.check();
   if(isDisposed())return;
   if(getDraft()?.id!==id||getDraft()?.revision!==revision||getDraft().name.trim()!==name)throw new Error('Setup/name changed during checking. Request save again for the newer work.');
   if(!check||check.authoringRevision!==revision||check.build!==build())throw new Error('Save requires a matching Ready check. Correct the review issues or retry the check.');
   const duplicate=nameError();if(duplicate)throw new Error(duplicate);
   const school={id:crypto.randomUUID(),name,definition:structuredClone(check.definition),definitionRevision:check.definitionRevision,build:check.build,authoring:structuredClone(getDraft()),check:structuredClone(check),sourceSchoolId:draft.source?.id??null,savedAt:new Date().toISOString()};
   cancelAutosave();
   repository.write(state=>{
    state.schools[school.id]=school;
    // The exact requested revision is consumed, never another tab's successor.
    const stored=state.setupDrafts?.[id];if(stored&&stored.revision!==revision)throw new Error('Setup revision changed before save. Recover the newer draft.');
    delete state.setupDrafts?.[id];
    for(const key of Object.keys(state.setupSelection??{}))if(state.setupSelection[key]===id)delete state.setupSelection[key];
    state.selected={schoolId:school.id,timetableId:null};
   },{atomic:true});
   onSaved(school);
  }catch(error){notify(error.message);}finally{saving=false;}
 }
 return{save};
}
