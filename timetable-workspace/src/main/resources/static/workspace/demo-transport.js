let csrf;
async function token() {
  const response = await fetch('/api/csrf');
  if (!response.ok) throw new Error('The temporary session could not open. Your browser data is retained. Retry shortly.');
  csrf = await response.json();
}
export async function demoRequest(path, { method = 'GET', body, headers = {} } = {}, retry = true) {
  if (method !== 'GET' && !csrf) await token();
  const response = await fetch(path, { method, headers: { ...(body ? { 'Content-Type': 'application/json' } : {}),
    ...headers, ...(method !== 'GET' ? { [csrf.headerName]: csrf.token } : {}) }, body: body === undefined ? undefined : typeof body === 'string' ? body : JSON.stringify(body) });
  const value = await response.json();
  if (!response.ok) {
    if (retry && ['SESSION_ENDED', 'REQUEST_FORBIDDEN'].includes(value.code)) {
      await token();
      if (value.code === 'SESSION_ENDED') await demoRequest('/api/demo/session', { method: 'POST' }, false);
      return demoRequest(path, { method, body, headers }, false);
    }
    const error=new Error(value.message || value.detail || 'The request failed. Your browser data is retained. Retry shortly.');
    error.status=response.status;error.code=value.code;error.validationReport=value.validationReport;throw error;
  }
  return { value, etag: response.headers.get('ETag') };
}
