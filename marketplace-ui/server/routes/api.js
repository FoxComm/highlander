import proxy from 'koa-proxy';

const matchUriRegexp = new RegExp('^/api/v1/');

export default function makeApiProxy() {
  const host = process.env.API_URL;

  return proxy({
    host,
    match: matchUriRegexp,
  });
}
