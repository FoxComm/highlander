'use strict';

const
  Router  = require('koa-router'),
  t       = require('thunkify-wrap'),
  request = require('request');

module.exports = function(app) {
  const
    config      = app.config.api,
    baseRequest = request.defaults({
      baseUrl: config.uri,
      _json: true
    });

  let router = new Router({
    prefix: `/api/${config.version}`
  });

  router.get('/:path*', function *() {
    let res = yield t(baseRequest.get)(this.params.path);
    this.status = res[0].statusCode;
    this.body = res[1];
  });

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
