import proxy from 'koa-proxy';

const matchUriRegexp = new RegExp(`^/api/v1/`);

export function makeApiProxy() {
  const host = process.env.PHOENIX_URL
    ? process.env.PHOENIX_URL
    : 'http://localhost:9090';

  return proxy({
    host,
    match: matchUriRegexp,
    map: path => path.replace(/^\/api\//, '/'),
  });
}
