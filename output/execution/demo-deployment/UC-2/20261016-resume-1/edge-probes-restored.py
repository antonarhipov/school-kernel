import concurrent.futures,datetime,http.client,json,ssl,urllib.parse
from pathlib import Path
host='demo-smoke-production.up.railway.app'

def request(name,path='/healthz',headers=(),method='GET',https=True,accepted=(200,)):
 c=(http.client.HTTPSConnection(host,context=ssl.create_default_context(),timeout=20) if https else http.client.HTTPConnection(host,timeout=20))
 try:
  c.putrequest(method,path,skip_host=True);c.putheader('Host',host)
  for k,v in headers:c.putheader(k,v)
  c.endheaders();r=c.getresponse();body=r.read().decode(errors='replace');hs={k.lower():v for k,v in r.getheaders()}
  result={'name':name,'status':r.status,'contentType':hs.get('content-type'),'body':body[:700],'setsCookie':'set-cookie' in hs,'location':hs.get('location')}
  assert r.status in accepted,result
  assert 'set-cookie' not in hs,result
  if r.status==403:assert 'acceptedBaseline' not in body,result
  return result
 finally:c.close()

probes=[]
for name,headers in [('normal',()),('client-http-scheme',(('X-Forwarded-Proto','http'),)),('client-foreign-forwarded-host',(('X-Forwarded-Host','evil.example'),)),('duplicate-client-scheme',(('X-Forwarded-Proto','http'),('X-Forwarded-Proto','https'))),('client-forwarded-ip',(('X-Forwarded-For','203.0.113.1'),('X-Real-IP','203.0.113.2')))]:
 probes.append(request(name,headers=headers))
probes.append(request('forwarded-rejected',headers=(('Forwarded','proto=https;host=evil.example'),),accepted=(403,)))
probes.append(request('foreign-origin-rejected','/api/workspace',headers=(('Origin','https://evil.example'),),accepted=(403,)))
probes.append(request('missing-origin-csrf-rejected','/api/workspace/clear',method='POST',accepted=(403,)))
probes.append(request('public-http-redirect',https=False,accepted=(301,302,307,308)))
# Independent connections exercise more of the edge pool than a single keep-alive client.
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
 probes+=list(pool.map(lambda i:request('concurrent-'+str(i)),range(64)))
p=Path('output/execution/demo-deployment/UC-2/20261016-resume-1/edge-probes-restored.json');p.write_text(json.dumps({'date':datetime.datetime.now(datetime.timezone.utc).isoformat(),'probes':probes,'verdict':'PASS'},indent=2)+'\n')
print('PASS:',len(probes),'stateless public edge/security probes; no visitor workspace created')
