'use strict';

const
  Router = require('koa-router');

module.exports = function(app) {
  let router = new Router();

  // @todo this will be reenabled once auth is available
  // - Tivs
  // router.use(app.requireAdmin);

  router.get('/:path*', function *(next) {
    this.theme = 'admin';
    yield next;
  }, app.renderReact);

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
