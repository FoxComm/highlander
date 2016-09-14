import proxy from 'koa-proxy';

export default function makeApiProxy() {
  const host = process.env.API_URL;

  return proxy({
    host,
    match: /^\/api\//,
    map: (path) => path.replace(/^\/api/, ''),
  });
}
