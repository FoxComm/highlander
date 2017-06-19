const path = require('path');
const koa = require('koa');
const co = require('co');
const serve = require('koa-better-static');
const convert = require('koa-convert');
const Config = require(path.resolve('config'));

const app = new koa();

const buildDir = path.resolve(__dirname, '../build');

app.init = co.wrap(function*(env) {
  if (env) {
    app.env = env;
  }

  app.config = new Config(app.env);

  if (app.env === 'production') {
    app.use(convert(serve(buildDir, { index: 'index.html' })));
  }

  if (app.env !== 'production') {
    require('./hmr')(app);
    app.use(require('koa-logger')());
  }

  require(`./middleware`)(app);

  // Without nginx we use `api` middleware to proxy api requests to API_URL
  if (process.env.API_URL) {
    require(`./api`)(app);
  }

  app.use(app.injectToken).use(app.renderLayout);

  app.server = app.listen(app.config.server.port);
});

if (!module.parent) {
  app.init().catch(function(err) {
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

if (!process.env.GA_TRACKING_ID && app.env === 'production') {
  console.warn(
    'WARNING. There is no google analytics tracking id configured.' + 'Use GA_TRACKING_ID env variable for that.'
  );
}

module.exports = app;
