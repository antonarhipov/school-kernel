#!/usr/bin/env python3
"""Published CLI acceptance for setup catalog 10. Writes isolated inputs, outputs and ledger."""
import copy,hashlib,json,pathlib,subprocess,time
ROOT=pathlib.Path(__file__).resolve().parents[2]
OUT=ROOT/'output/kernel/school-setup-editor'/str(int(time.time()*1000)); OUT.mkdir(parents=True)
JAVA='/Users/anton/Library/Java/JavaVirtualMachines/jbr-25.0.2/Contents/Home/bin/java'
JAR=ROOT/'kernel-cli/target/school-kernel.jar'
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2));return p
def call(name,op,d,a=None,result=None):
 output=OUT/(name+'-'+op+'.json');args=[JAVA,'-jar',str(JAR),op,'--definition',str(d),'--output',str(output)]
 if a:args+=['--assignments',str(a)]
 if result:args+=['--result',str(result)]
 if op=='plan':args+=['--time-limit','1s']
 p=subprocess.run(args,capture_output=True,text=True);(OUT/(name+'-'+op+'.log')).write_text(p.stdout+p.stderr)
 assert output.exists(),(args,p.stderr)
 return json.loads(output.read_text()),output
# Each day's sequence is maximal; missing position explicitly tests the full-grid adjacency.
cases=[([],[[1],[1],[1],[1],[1]],True),([],[[1,2]],False),([(2,'EXACTLY',1)],[[1,2],[1],[1],[1]],True),([(2,'EXACTLY',1)],[[1,2,3],[1],[1]],False),([(2,'EXACTLY',2)],[[1,2],[1,2]],True),([(2,'EXACTLY',2)],[[1,2,3,4]],False),([(2,'AT_LEAST',1)],[[1,2],[1,2]],True),([(2,'EXACTLY',1)],[[1,2],[1,2]],False),([(3,'EXACTLY',1)],[[1,2,3],[1]],True),([(2,'EXACTLY',1),(3,'EXACTLY',1)],[[1,2],[1,2,3]],True),([(2,'EXACTLY',1)],[[1,2]],True),([(2,'EXACTLY',1)],[[1,2]],False),([(2,'EXACTLY',1)],[[1,3]],False),([(2,'EXACTLY',1)],[[1,2]],False),([(2,'EXACTLY',1)],[[1,2]],False),([(4,'EXACTLY',1)],[[1,2,3,4]],True),([(3,'EXACTLY',1)],[[1,2]],False)]
ledger=['# UC-13 / UC-12 published CLI ledger','',f'Kernel SHA256: `{hashlib.sha256(JAR.read_bytes()).hexdigest()}`','', '| Example | Review | Verify forged/valid result | Generation |','| --- | --- | --- | --- |']
for n,(rules,days,valid) in enumerate(cases,1):
 d={'schemaVersion':1,'catalogVersion':10,'schoolId':'block-school','displayName':'Blocks','subjects':[{'id':'s','displayName':'Subject'}],'teachers':[{'id':t,'displayName':t,'qualifiedSubjectIds':['s']} for t in ['t','t2']],'cohorts':[{'id':'c','displayName':'Class','size':1,'maxDailyGaps':2147483647}],'rooms':[{'id':r,'displayName':r,'capacity':2,'capabilityIds':[]} for r in ['r','r2']],'periods':[],'lessons':[],'consecutiveBlocks':[{'id':f'b{i}','cohortId':'c','subjectId':'s','length':l,'operator':op,'weeklyCount':count} for i,(l,op,count) in enumerate(rules)]};a=[]
 # Example17 has a triple demand, but only a double observed and a separated single.
 if n==17:days=days+[[1]]
 for day,positions in enumerate(days):
  for order in range(1,6):d['periods'].append({'id':f'p{day}-{order}','displayName':str(order),'weekday':['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY'][day],'order':order,'adjacentToNext':order<5 and not(n==12 and order==1)})
  for pos in positions:
   lid=f'l{len(a)}';teacher='t2' if n==14 and len(a)==1 else 't';room='r2' if n==15 and len(a)==1 else 'r'
   d['lessons'].append({'id':lid,'displayName':lid,'subjectId':'s','cohortId':'c','teacherId':teacher,'seriesId':'series'})
   a.append({'lessonId':lid,'subjectId':'s','cohortId':'c','teacherId':teacher,'periodId':f'p{day}-{pos}','roomId':room})
 dp=write(OUT/f'{n}-definition.json',d);ap=write(OUT/f'{n}-assignments.json',a)
 review,rp=call(str(n),'review',dp,ap);assert review.get('valid')==valid,(n,review)
 if not valid:assert any(c['constraintId']=='hard.consecutive-block' for c in review['conflicts']),(n,review)
 # Obtain complete valid result shape and provenance from a no-block legacy review of this same arrangement.
 legacy=copy.deepcopy(d);legacy['catalogVersion']=9;legacy.pop('consecutiveBlocks');[p.pop('adjacentToNext') for p in legacy['periods']]
 lp=write(OUT/f'{n}-legacy-definition.json',legacy);lr,_=call(str(n)+'-legacy','review',lp,ap);assert lr['valid'];assert lr['softMatchCounts']['soft.series-same-day']==sum(max(0,len(x)-1) for x in days)
 result=review.get('result',copy.deepcopy(lr['result']));result['catalogVersion']=10;result['inputRevision']=review['definitionRevision'];result['schoolId']=d['schoolId']
 scope={'schemaVersion':1,'schoolId':d['schoolId'],'inputRevision':result['inputRevision'],'assignments':sorted(a,key=lambda x:x['lessonId'])}
 result['timetable']['assignments']=a;result['timetableRevision']='sha256:'+hashlib.sha256(json.dumps(scope,sort_keys=True,separators=(',',':'),ensure_ascii=False).encode()).hexdigest()
 resultpath=write(OUT/f'{n}-result.json',result);verified,_=call(str(n),'verify',dp,result=resultpath);assert (verified['status']=='VERIFIED')==valid,(n,verified)
 generated='not needed (invalid arrangement)'
 if valid:
  generation,gp=call(str(n),'plan',dp);assert generation['status']=='FEASIBLE',(n,generation)
  check,_=call(str(n)+'-generated','verify',dp,result=gp);assert check['status']=='VERIFIED';generated='PASS'
 # Mandatory group exemption; AT_LEAST extras retain one extra same-day unit.
 if valid:
  expected=max(0,sum(max(0,len(x)-1) for x in days)-sum((l-1)*k for l,op,k in rules));assert review['softMatchCounts']['soft.series-same-day']==expected,(n,review)
 ledger.append(f'| {n} | PASS ({valid}) | PASS | {generated} |')
# Full original grid controls adjacency even when the solver removes reserved values or class availability excludes them.
for label in ['filtered-reserved-opportunity','class-lunch-opportunity']:
 d=json.loads((OUT/'13-definition.json').read_text());a=json.loads((OUT/'13-assignments.json').read_text())
 if label.startswith('filtered'):d['reservedPeriodIds']=['p0-2']
 else:d['cohorts'][0]['availablePeriodIds']=['p0-1','p0-3']
 dp=write(OUT/(label+'-definition.json'),d);ap=write(OUT/(label+'-assignments.json'),a);review,_=call(label,'review',dp,ap);assert not review['valid'];assert review['hardMatchCounts']['hard.consecutive-block']>0
# Mandatory exemption is weekly and bounded; separated same-day singles/grouping still cost spreading.
d=json.loads((OUT/'7-definition.json').read_text());a=json.loads((OUT/'7-assignments.json').read_text())
for assignment,pos in zip(a,[1,2,4,5]):assignment['periodId']=f'p0-{pos}'
dp=write(OUT/'extra-same-day-definition.json',d);ap=write(OUT/'extra-same-day-assignments.json',a);review,_=call('extra-same-day','review',dp,ap);assert review['valid'] and review['softMatchCounts']['soft.series-same-day']==2
# Counts sum across teachers, while each maximal block remains internally homogeneous.
d=json.loads((OUT/'5-definition.json').read_text());a=json.loads((OUT/'5-assignments.json').read_text())
for lesson,assignment in zip(d['lessons'][2:],a[2:]):lesson['teacherId']=assignment['teacherId']='t2'
dp=write(OUT/'across-teachers-definition.json',d);ap=write(OUT/'across-teachers-assignments.json',a);review,_=call('across-teachers','review',dp,ap);assert review['valid']
ledger+=['','Additional full-grid filtered-reserved/class-lunch boundaries, bounded same-day spreading and valid weekly frequencies across teachers: PASS.']
# Located malformed metadata and assignment identity failures never produce a valid review.
d=json.loads((OUT/'3-definition.json').read_text());a=json.loads((OUT/'3-assignments.json').read_text())
for label,mutate in [('duplicate-rule',lambda x:x['consecutiveBlocks'].append(copy.deepcopy(x['consecutiveBlocks'][0]))),('unknown-class',lambda x:x['consecutiveBlocks'][0].update(cohortId='missing')),('excess-consumption',lambda x:x['consecutiveBlocks'][0].update(weeklyCount=999)),('missing-adjacency',lambda x:x['periods'][0].pop('adjacentToNext')),('invalid-last-adjacency',lambda x:x['periods'][-1].update(adjacentToNext=True))]:
 bad=copy.deepcopy(d);mutate(bad);bp=write(OUT/(label+'.json'),bad);review,_=call(label,'verify',bp);assert review['status']=='INVALID_INPUT',review
for label,values in [('missing',a[:-1]),('duplicate',a+[a[0]]),('extra',a+[dict(a[0],lessonId='unknown')]),('mismatch',[dict(a[0],teacherId='t2')]+a[1:])]:
 ap=write(OUT/(label+'.json'),values);review,_=call(label,'review',OUT/'3-definition.json',ap);assert review['status']=='INVALID_INPUT',review
# Forced undeclared double: bounded generation cannot publish a feasible timetable.
d=json.loads((OUT/'2-definition.json').read_text())
for lesson,assignment in zip(d['lessons'],json.loads((OUT/'2-assignments.json').read_text())):lesson['periodLock']=assignment['periodId']
bp=write(OUT/'forced-undeclared.json',d);failure,_=call('forced-undeclared','plan',bp);assert failure['status']=='NO_FEASIBLE_SOLUTION_FOUND',failure
ledger+=['','Malformed/duplicate/reference/consumption/adjacency metadata: PASS. Missing/duplicate/extra/mismatched assignments: PASS. Forced undeclared double generation returns bounded NO_FEASIBLE_SOLUTION: PASS.']
(OUT/'ledger.md').write_text('\n'.join(ledger)+'\n\nPASS. Each review and verification ran a separate published CLI process; no internal-only validity assertion. Example 11 represents an ordinary break through explicit true adjacency; example 12 declares a lunch boundary. All outputs and legacy scoring comparisons retained.\n')
print(OUT)
