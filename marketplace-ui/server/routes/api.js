import proxy from 'koa-proxy';

export default function makeApiProxy() {
  const host = process.env.API_URL;

  return proxy({
    host,
    match: /^\/mkt\//,
    map: (path) => path.replace(/^\/mkt/, ''),
  });
}
