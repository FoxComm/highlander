const proxy = require('koa-proxy');
const convert = require('koa-convert');

module.exports = function(app) {
  const config = app.config.api;
  const matchUriRegexp = new RegExp(`^/api/`);

  app.use(async function apiHandler(ctx, next) {
    if (ctx.request.url.match(matchUriRegexp)) {
      ctx.request.headers['Accept'] = 'application/json';

      await app.jsonError.call(ctx, next);
    } else {
      next();
    }
  });

  app.use(convert(proxy({
    host: config.host,
    match: matchUriRegexp,
  })));
};
