import fs from 'node:fs';
import {schoolDocumentSchema as authoring} from '../../timetable-workspace/src/main/resources/static/workspace/school-document-schema.js';
const schema=JSON.parse(fs.readFileSync('kernel-contract/src/main/resources/schema/school-definition-v1.schema.json'));
schema.$id='https://school-kernel.invalid/schema/school-definition-v2.schema.json';schema.title='School Kernel audience definition v2';
schema.properties.schemaVersion={const:2};schema.properties.catalogVersion={const:11};schema.allOf[0].if.properties.catalogVersion={const:11};
schema.$defs.teacher.properties.eligibleClassIdsBySubject={type:'object',additionalProperties:{$ref:'#/$defs/idArray'}};
schema.$defs.teacher.required.push('eligibleClassIdsBySubject');schema.$defs.teacher.properties.referenceWeeklyMaximum={type:'integer',minimum:0,maximum:2147483647};
schema.$defs.cohort.properties.splits=structuredClone(authoring.properties.classes.items.properties.splits);
schema.$defs.cohort.properties.relationships=structuredClone(authoring.properties.classes.items.properties.relationships);
schema.$defs.period.properties.gapBoundaryAfter={type:'boolean'};
schema.properties.sessionLocks={type:'array',items:{type:'object',additionalProperties:false,required:['sessionId'],properties:{sessionId:{type:'string'},periodId:{type:'string'},roomId:{type:'string'}},anyOf:[{required:['periodId']},{required:['roomId']}]}};
schema.properties.audiencePlans=structuredClone(authoring.properties.audiencePlans);schema.required.push('audiencePlans');
function complete(value){if(!value||typeof value!=='object')return;if(Array.isArray(value.type)&&value.type.includes('integer')){value.type='integer';value.minimum=0;value.maximum=2147483647;}for(const child of Object.values(value))complete(child);}
schema.properties.coordinationRules=structuredClone(authoring.properties.coordinationRules);complete(schema.properties.coordinationRules);complete(schema.properties.audiencePlans);complete(schema.$defs.cohort.properties.splits);complete(schema.$defs.cohort.properties.relationships);
const plan=schema.properties.audiencePlans.items;plan.properties.portions.items.properties.sessionIds.uniqueItems=true;
const policy=plan.properties.portions.items.properties.teacherPolicy;
policy.allOf=[{if:{properties:{mode:{const:'fixed'}}},then:{required:['fixedTeacherId']}},{if:{properties:{mode:{const:'sharing'}}},then:{required:['shares'],properties:{shares:{minItems:1}}}}];
fs.writeFileSync('kernel-contract/src/main/resources/schema/school-definition-v2.schema.json',JSON.stringify(schema,null,2)+'\n');
