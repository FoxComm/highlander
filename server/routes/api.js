import proxy from 'koa-proxy';

const matchUriRegexp = new RegExp(`^/api/v1/`);

function toBase64(str) {
  return new Buffer(str).toString('base64');
}

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

export function makeApiHandler() {
  // TODO: Remove once auth fully lands.
  const user = 'admin@admin.com';
  const password = 'password';
  const basicAuthHeader = toBase64(`${user}:${password}`);

  return function *apiHandler(next) {
    if (this.request.url.match(matchUriRegexp)) {
      this.request.headers.Accept = 'application/json';
      this.request.headers.Authorization = `Basic ${basicAuthHeader}`;
    }

    yield next;
  };
}
