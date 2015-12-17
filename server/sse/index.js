const
  Router  = require('koa-router'),
  Api     = require('../lib/api'),
  koaBody = require('koa-body'),
  url     = require('url'),
  stream  = require('stream'),
  http    = require('http');

module.exports = function(app) {
  const config = app.config.api;

  let router = new Router({
    prefix: `/sse/${config.version}`
  });

  router.all('/:path*', function *() {
    console.log('request received');

    let options = url.parse('http://localhost:9090/v1/notifications/1');
    options.headers = this.req.headers;
    options.method = this.req.method;
    options.agent = false;

    console.log(options);

    let writestream = new stream.Stream();
    writestream.writable = true;
    writestream.write = function (data) {
      console.log(data);
      return true; // true means 'yes i am ready for more data now'
      // OR return false and emit('drain') when ready later
    };
    writestream.end = function (data) {
      // no more writes after end
      // emit "close" (optional)
    };

    var connector = http.get(options, function(response) {
      response.on('data', function(chunk) {
        writestream.write(chunk);
      });
    });

    this.status = 200;
    this.type = 'text/event-stream';
    this.body = writestream;
  });

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
