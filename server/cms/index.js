const Router  = require('koa-router');

module.exports = function(app) {
  const router = new Router();

  router.use(app.requireAdmin);

  router
    .get('/login', app.renderLogin, app.renderLayout('login'))
    .get('/:path*', app.renderReact, app.renderLayout('admin'));

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
