const Router  = require('koa-router');

module.exports = function(app) {
  const router = new Router();

  router.use(app.requireAdmin);

  router
    .get('/:path*', app.renderReact, app.renderLayout);

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
