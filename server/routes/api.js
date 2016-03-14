import proxy from 'koa-proxy';

const matchUriRegexp = new RegExp(`^/api/v1/`);

export function makeProxy() {
  return proxy({
    host: 'http://192.168.10.111:9090',
    match: matchUriRegexp,
    map: path => path.replace(/^\/api\//, '/'),
  });
}
