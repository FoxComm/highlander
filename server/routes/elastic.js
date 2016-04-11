import proxy from 'koa-proxy';

const matchUriRegexp = new RegExp(`^/api/search/`);

export function makeElasticProxy() {
  const host = process.env.ELASTIC_URL;

  return proxy({
    host,
    match: matchUriRegexp,
  });
}
