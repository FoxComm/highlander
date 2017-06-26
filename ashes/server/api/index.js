const proxy = require('koa-proxy');
const convert = require('koa-convert');

module.exports = function(app) {
  const matchUriRegexp = new RegExp(`^/api/`);

  if (!process.env.API_URL) {
    return;
  }

  app.use(async function apiHandler(ctx, next) {
    if (ctx.request.url.match(matchUriRegexp)) {
      ctx.request.headers['Accept'] = 'application/json';

      await app.jsonError.call(ctx, next);
    } else {
      next();
    }
  });

  app.use(
    convert(
      proxy({
        host: process.env.API_URL,
        match: matchUriRegexp,
      })
    )
  );
};
