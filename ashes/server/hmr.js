const proxy = require('koa-proxy');
const convert = require('koa-convert');

// webpack-dev-server serves from 4001 port,
// so, we need to proxy all hmr-related requests
module.exports = function(app) {
  app.use(convert(proxy({
    host: `http://localhost:${process.env.WEBPACK_PORT}`,
    match: /^\/admin\/.*\.(js|css|woff|svg|ico|json)$/,
  })));
};
