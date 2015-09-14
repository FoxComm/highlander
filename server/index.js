'use strict';

const auth    = require('koa-basic-auth');
const koa     = require('koa');
const path    = require('path');
const co      = require('co');
const favicon = require('koa-favicon');
const serve   = require('koa-static');
const Config  = require(path.resolve('config'));

const app = koa();

app.init = co.wrap(function *(env) {
  if (env) { app.env = env; }
  app.config = new Config(app.env);
  if (app.env == 'staging') {
    app.use(function *(next) {
      try { 
        yield next;
      } catch (err) {
        if (401 == err.status) {
          this.status = 401;
          this.set('WWW-Authenticate', 'Basic');
          this.body = 'the fox says you cannot pass';
        } else {
          throw err;
        }
      }
    });

    app.use(auth({ name: 'foxcomm', pass: 'st@ging' }));
  }
  app.use(serve(app.config.server.publicDir));
  app.use(favicon(app.config.layout.favicon));
  app.seeds = yield* require(`${__dirname}/seeds`)();
  if (app.env !== 'production') {
    app.use(require('koa-logger')());
  }
  
  require(`${__dirname}/middleware`)(app);
  require(`${__dirname}/api`)(app);
  require(`${__dirname}/fauxnix`)(app);
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
