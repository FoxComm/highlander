'use strict';

const
  Router = require('koa-router');

module.exports = function(app) {
  let router = new Router({
    prefix: '/api/v1'
  });

  router.get('/:path*', function *() {
    this.body = this.params.path;
  });

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
