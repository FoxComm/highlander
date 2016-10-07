import proxy from 'koa-proxy';

const matchUriRegexp = new RegExp(`^/api/search/`);

export function makeElasticProxy() {
  const host = process.env.API_URL;

  return proxy({
    host,
    match: matchUriRegexp,
  });
}
