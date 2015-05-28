'use strict';

const
  Router  = require('koa-router'),
  t       = require('thunkify-wrap'),
  request = require('request');

module.exports = function(app) {
  const
    baseRequest = request.defaults({
      baseUrl: app.config.api.uri,
      _json: true
    });

  let router = new Router({
    prefix: '/api/v1'
  });

  router.get('/:path*', function *() {
    let
      res   = yield t(baseRequest.get)(this.params.path),
      data  = res[1];
    this.body = data;
  });

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
