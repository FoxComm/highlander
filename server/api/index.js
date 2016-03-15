
const proxy = require('koa-proxy');
const koaBody = require('koa-body');

function toBase64(str) {
  return new Buffer(str).toString('base64');
}

module.exports = function(app) {
  const config = app.config.api;
  const matchUriRegexp = new RegExp(`^/api/${config.version}/`);

  app.use(function *apiHandler(next) {
    if (this.request.url.match(matchUriRegexp)) {
      this.request.headers['Accept'] = 'application/json';

      yield app.jsonError.call(this, next);
    } else {
      yield next;
    }
  });

  app.use(proxy({
    host:  config.host,
    match: matchUriRegexp,
    map: function(path) {
      return path.replace(/^\/api\//, '/');
    }
  }));

};
