const proxy = require('koa-proxy');

// webpack-dev-server serves from 4001 port,
// so, we need to proxy all hmr-related requests
module.exports = function(app) {
  app.use(proxy({
    host: `http://localhost:${process.env.WEBPACK_PORT}`,
    match: /^\/admin/,
  }));
};
