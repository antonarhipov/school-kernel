import subprocess,re
from pathlib import Path
base=['node','/Users/anton/.npm/_npx/31e32ef8478fbf80/node_modules/@playwright/cli/playwright-cli.js','--session','authoring-uc20-r1'];out=Path('output/playwright/school-definition-authoring/UC-20/20261012-7')
def cli(name,*args):
 r=subprocess.run(base+list(args),text=True,capture_output=True);(out/(name+'.log')).write_text(r.stdout+r.stderr);assert r.returncode==0 and not r.stdout.startswith('### Error'),(name,r.stdout[:300]);return r.stdout
snapshot=cli('manual-snapshot','snapshot');ref=re.search(r'button "Discard manual draft"[^\n]*\[ref=([^\]]+)\]',snapshot).group(1);cli('manual-confirm','click',ref);cli('manual-cancel','dialog-dismiss');cli('manual-cancel-values','run-code','async page => {return {snapshot:await(await page.request.get("http://127.0.0.1:18110/api/workspace")).json()};}');snapshot=cli('manual-retry-snapshot','snapshot');ref=re.search(r'button "Discard manual draft"[^\n]*\[ref=([^\]]+)\]',snapshot).group(1);cli('manual-retry-confirm','click',ref);cli('manual-accept','dialog-accept')
