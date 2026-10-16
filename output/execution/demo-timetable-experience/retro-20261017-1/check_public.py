"""Stateless HTTPS/static and public edge verification; never retains cookies."""
import datetime, hashlib, json, time, urllib.error, urllib.request
from pathlib import Path
import argparse
parser=argparse.ArgumentParser();parser.add_argument('--output',type=Path,required=True);args=parser.parse_args()
EV=args.output;EV.mkdir(parents=True,exist_ok=True)
ROOT=Path.cwd()
BASE='https://demo-smoke-production.up.railway.app'
class NoRedirect(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,*args):return None
opener=urllib.request.build_opener(NoRedirect())
def request(path='/healthz',headers=None,method='GET',url=None):
 req=urllib.request.Request(url or BASE+path,headers=headers or {},method=method,data=b'{}' if method=='POST' else None)
 try:r=opener.open(req,timeout=20)
 except urllib.error.HTTPError as error:r=error
 with r:
  body=r.read();return r.status,body,{k.lower():v for k,v in r.headers.items()}
observations=[]
for n in range(40):
 path='/healthz' if n%2==0 else '/workspace/demo-panel.js'
 status,body,headers=request(path);observations.append({'path':path,'status':status,'setsCookie':'set-cookie' in headers})
 assert status==200 and 'set-cookie' not in headers,(path,status)
 if n%2==0:assert json.loads(body)=={'status':'UP'}
 time.sleep(.25)
probes=[]
cases=[('normal',{},'GET',None,200),('client-http-scheme',{'X-Forwarded-Proto':'http'},'GET',None,200),('client-foreign-forwarded-host',{'X-Forwarded-Host':'foreign.invalid'},'GET',None,200),('duplicate-client-scheme',{'X-Forwarded-Proto':'https,http'},'GET',None,200),('client-forwarded-ip',{'X-Forwarded-For':'127.0.0.1'},'GET',None,200),('forwarded-rejected',{'Forwarded':'for=127.0.0.1;proto=https;host=foreign.invalid'},'GET',None,403),('foreign-origin-rejected',{'Origin':'https://foreign.invalid'},'GET',None,403),('missing-origin-csrf-rejected',{},'POST',None,403),('public-http-redirect',{},'GET','http://demo-smoke-production.up.railway.app/healthz',301)]
for name,headers,method,url,expected in cases:
 status,body,response=request('/api/demo/definition' if method=='POST' else '/healthz',headers,method,url)
 value={'name':name,'status':status,'body':body.decode(),'setsCookie':'set-cookie' in response,'location':response.get('location')};probes.append(value)
 assert status==expected and not value['setsCookie'],value
 if status==403:assert json.loads(body)['code']=='PUBLIC_REQUEST_DENIED'
 if status==301:assert value['location']==BASE+'/healthz'
assets=[]
for asset in (ROOT/'timetable-workspace/src/main/resources/static/workspace').iterdir():
 if not asset.is_file():continue
 path='/workspace/'+asset.name;expected=hashlib.sha256(asset.read_bytes()).hexdigest();status,body,headers=request(path);value=hashlib.sha256(body).hexdigest();assets.append({'path':path,'status':status,'sha256':value,'matchesPinned':value==expected})
 assert status==200 and value==expected and 'set-cookie' not in headers
status,body,headers=request('/api/demo/timetables');assert status==200 and 'set-cookie' not in headers
bundle=json.loads(body);assert bundle['build']==json.loads((ROOT/'timetable-workspace/src/main/resources/demo/timetables/scoring-evidence.json').read_text())['kernelBuild']
assert [s['identifier'] for s in bundle['schools']]==['two-class','full','star-wars']
pairs=[]
for school in bundle['schools']:
 id=school['identifier'];assert school['definition']==json.loads((ROOT/f'timetable-workspace/src/main/resources/demo/timetables/{id}-definition.json').read_text())
 for variant in school['timetables']:
  assert variant['result']==json.loads((ROOT/f'timetable-workspace/src/main/resources/demo/timetables/{id}-{variant["id"]}-result.json').read_text());pairs.append(id+'/'+variant['id'])
assert len(pairs)==6
result={'kernelBuild':bundle['build'],'exactSamplePairs':pairs,'date':datetime.datetime.now(datetime.timezone.utc).isoformat(),'verdict':'PASS','statelessResponses':observations,'edgeProbes':probes,'staticAssets':assets,'disposition':'No session created; only stateless health/static and refused/redirected edge requests'}
(EV/'cleanup-public-checks.json').write_text(json.dumps(result,indent=2)+'\n')
print('PASS: 40 consecutive health/static reads, 9 edge/spoof probes, all pinned static hashes and six exact timetable pairs; no cookies')
