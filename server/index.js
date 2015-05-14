'use strict';

const
  koa     = require('koa'),
  path    = require('path'),
  co      = require('co'),
  favicon = require('koa-favicon'),
  serve   = require('koa-static');

const app = koa();

app.init = co.wrap(function *() {
  app.config = require(path.resolve('config'));
  app.use(serve(app.config.server.publicDir));
  app.use(favicon(app.config.layout.favicon));
  if (app.env === 'development') {
    app.use(require('koa-logger')());
  }
  require(`${__dirname}/middleware`)(app);
  require(`${__dirname}/cms`)(app);
  require(`${__dirname}/web`)(app);
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
