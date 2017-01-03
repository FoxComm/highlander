const path = require('path');
const koa = require('koa');
const co = require('co');
const favicon = require('koa-favicon');
const serve = require('koa-better-static');
const Config = require(path.resolve('config'));
const log4js = require('koa-log4');

require('babel-polyfill');
require('../src/postcss').installHook();

const app = koa();

app.init = co.wrap(function *(env) {
  if (env) { app.env = env; }

  log4js.configure(path.join(`${__dirname}`, '../log4js.json'));

  app.config = new Config(app.env);
  app.use(log4js.koaLogger(log4js.getLogger('http'), { level: 'auto' }));
  app.use(serve(app.config.server.publicDir));
  app.use(favicon(app.config.layout.favicon));
  if (app.env.environment !== 'production') {
    app.use(require('koa-logger')());
  }

  require(`${__dirname}/middleware`)(app);
  require(`${__dirname}/api`)(app);
  require(`${__dirname}/cms`)(app);
  app.server = app.listen(app.config.server.port);
});

if (!module.parent) {
  app.init().catch(function (err) {
    console.error(err.stack);
    process.exit(1);
  });
}

process.on('message', function(msg) {
  if (msg === 'shutdown') {
    app.server.close(function() {
      process.exit(0);
    });
  }
});

module.exports = app;
