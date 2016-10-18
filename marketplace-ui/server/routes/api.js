import proxy from 'koa-proxy';

export default function makeApiProxy() {
  const host = process.env.API_URL;

  return proxy({
    host,
    match: /^\/api\/v1\/mkt\//,
    map: (path) => path.replace(/^\/api\/v1\/mkt/, ''),
  });
}
