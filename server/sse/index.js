const
  Router  = require('koa-router'),
  Api     = require('../lib/api'),
  koaBody = require('koa-body'),
  url     = require('url'),
  http    = require('http');

module.exports = function(app) {
  const config = app.config.api;

  let router = new Router({
    prefix: `/sse/${config.version}`
  });

  router.all('/:path*', function *() {
    console.log('request received');

    this.pause();

    let options = url.parse('http://localhost:9090/v1/notifications/1');
    options.headers = this.req.headers;
    options.method = this.req.method;
    options.agent = false;

    var connector = http.request(options);

    this.status = 200;
    this.type = 'text/event-stream';
    this.body = connector;
    this.resume();
  });

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
