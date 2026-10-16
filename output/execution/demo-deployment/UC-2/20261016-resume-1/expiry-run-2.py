import datetime,json,pathlib,subprocess,time,urllib.request
ROOT=pathlib.Path('/Users/anton/IdeaProjects/school-kernel/school-kernel')
OUT=ROOT/'output/execution/demo-deployment/UC-2/20261016-resume-1'
CLI='/private/tmp/school-demo-railway-npm/_npx/79fa66f96c8fdacf/node_modules/@railway/cli/bin/railway'
P='13d509af-1436-4024-bb64-f9f78fa439fc';E='a41a9c0e-ec3f-4ef4-9e08-996d998d1815';S='672cabae-b54e-4965-bbb2-5236bec81c33'
SELECT=['--project',P,'--environment',E,'--service',S]
BASE='https://demo-smoke-production.up.railway.app'
report={'startedAt':datetime.datetime.now(datetime.timezone.utc).isoformat()}

def call(*args):
 r=subprocess.run([CLI,*args],capture_output=True,text=True,timeout=60,check=True)
 return json.loads(r.stdout)

def deployment():
 d=call('deployment','list',*SELECT,'--limit','1','--json')[0]
 return {'id':d['id'],'status':d['status'],'imageDigest':d.get('meta',{}).get('imageDigest'),'manifest':d.get('meta',{}).get('serviceManifest')}

def ready(previous,expected):
 deadline=time.monotonic()+180
 while time.monotonic()<deadline:
  try:
   d=deployment()
  except (subprocess.SubprocessError, OSError, ValueError) as failure:
   print('Transient provider readiness read; retrying:',type(failure).__name__,flush=True)
   time.sleep(2)
   continue
  if d['id']!=previous and d['status']=='SUCCESS':
   try:
    with urllib.request.urlopen(BASE+'/api/demo/config',timeout=12) as r:cfg=json.load(r)
    if (cfg['idleMinutes'],cfg['absoluteHours'])==expected:return d
   except Exception:pass
  if d['id']!=previous and d['status'] in ['FAILED','CRASHED']:raise RuntimeError('Test redeploy failed: '+str(d))
  time.sleep(2)
 raise TimeoutError('Test service did not become ready with expected clock configuration')

values=call('variable','list',*SELECT,'--json')
# Retain only these non-secret test settings; never write or print the complete variable map.
keys=['DEMO_IDLE_TIMEOUT','DEMO_ABSOLUTE_TIMEOUT']
prior={k:values.get(k) for k in keys}
report['priorClockVariables']=prior
assert all(v is None for v in prior.values()), 'Unexpected prior clock overrides require deliberate preservation'
assert isinstance(values.get('SPRING_APPLICATION_JSON'),str)
original=json.loads(values['SPRING_APPLICATION_JSON'])
assert original=={'demo':{'max-workspaces':2}}, 'Preserve unexpected operator configuration'
previous=deployment()['id']
try:
 # Updating the existing JSON override makes restoration one atomic variable mutation, avoiding two redeploys.
 temporary={'demo':{'max-workspaces':2,'idle-timeout':'15s','absolute-timeout':'45s'}}
 call('variable','set','SPRING_APPLICATION_JSON='+json.dumps(temporary,separators=(',',':')),*SELECT,'--skip-deploys','--json')
 call('redeploy',*SELECT,'--yes','--json')
 report['expiryDeployment']=ready(previous,(0,0))
 report['testClock']={'idle':'15s','absolute':'45s','maxWorkspaces':2}
 print('Test-only 15s / 45s clock deployment ready',flush=True)
 output=ROOT/'output/playwright/demo-deployment/UC-2/public-expiry-20261016-2'
 command=['./mvnw','-q','-pl','timetable-workspace','-am','-Dit.test=DemoRemoteExpiryIT','-Dfailsafe.failIfNoSpecifiedTests=false','-Ddemo.remote.expiry=true','-Ddemo.remote.url='+BASE,'-Ddemo.walkthrough.dir='+str(output),'test-compile','failsafe:integration-test','failsafe:verify']
 with (OUT/'public-expiry-2.log').open('w') as log:
  result=subprocess.run(command,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,timeout=180)
 report['testExitCode']=result.returncode
 print('Expiry walkthrough exit code:',result.returncode,flush=True)
 # Capture only safe application logs already emitted by this isolated service.
 logs=subprocess.run([CLI,'logs',*SELECT,'--lines','100','--json'],capture_output=True,text=True,timeout=45,check=True)
 (OUT/'expiry-service.log').write_text(logs.stdout)
finally:
 try: previous=deployment()['id']
 except (subprocess.SubprocessError, OSError, ValueError): pass
 call('variable','set','SPRING_APPLICATION_JSON='+json.dumps(original,separators=(',',':')),*SELECT,'--skip-deploys','--json')
 call('redeploy',*SELECT,'--yes','--json')
 report['restoredDeployment']=ready(previous,(60,8))
 report['restoredClock']={'idle':'60m','absolute':'8h','maxWorkspaces':2}
 report['finishedAt']=datetime.datetime.now(datetime.timezone.utc).isoformat()
 (OUT/'expiry-and-restoration.json').write_text(json.dumps(report,indent=2)+'\n')
 print('Original 60m / 8h session configuration restored and ready',flush=True)
assert report.get('testExitCode')==0, 'Expiry walkthrough failed; defaults were restored'
