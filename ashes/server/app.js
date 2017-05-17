const path = require('path');
const koa = require('koa');
const co = require('co');
const favicon = require('koa-favicon');
const serve = require('koa-better-static');
const Config  = require(path.resolve('config'));

const app = koa();

const publicDir = path.resolve(__dirname + './../public');
const buildDir = path.resolve(__dirname + './../build');

app.init = co.wrap(function *(env) {
  if (env) {
    app.env = env;
  }

  app.config = new Config(app.env);

  app.use(serve(buildDir));
  app.use(serve(publicDir));
  app.use(favicon(path.resolve('public/admin/favicon.ico')));

  if (app.env.environment !== 'production') {
    require('./hmr')(app);
    app.use(require('koa-logger')());
  }

  require(`./middleware`)(app);

  // Without nginx we use `api` middleware to proxy api requests to API_URL
  if (!process.env.BEHIND_NGINX) {
    require(`./api`)(app);
  }

  require(`./cms`)(app);
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

if (!process.env.GA_TRACKING_ID && process.env.NODE_ENV === 'production') {
  console.warn('WARNING. There is no google analytics tracking id configured.' +
    'Use GA_TRACKING_ID env variable for that.');
}

module.exports = app;
