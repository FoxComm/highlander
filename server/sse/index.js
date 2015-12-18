const
  Router  = require('koa-router'),
  // sse     = require('koa-sse'),
  Api     = require('../lib/api'),
  koaBody = require('koa-body'),
  url     = require('url'),
  stream  = require('stream'),
  http    = require('http');

var Transform = require('stream').Transform;
var inherits = require('util').inherits;

inherits(SSE, Transform);

function SSE(options) {
  if (!(this instanceof SSE)) return new SSE(options);

  options = options || {};
  Transform.call(this, options);
}

SSE.prototype._transform = function (data, enc, cb) {
  this.push(data.toString('utf8'));
  cb();
};

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

    this.req.setTimeout(Number.MAX_VALUE);

    this.type = 'text/event-stream; charset=utf-8';
    this.set('Cache-Control', 'no-cache');
    this.set('Connection', 'keep-alive');

    var body = this.body = SSE();
    var stream = http.get(options);
    stream.pipe(body);

    // if the connection closes or errors,
    // we stop the SSE.
    var socket = this.socket;
    socket.on('error', close);
    socket.on('close', close);

    function close() {
      stream.unpipe(body);
      socket.removeListener('error', close);
      socket.removeListener('close', close);
    }

    // let writestream = new stream.Stream();
    // writestream.writable = true;
    // writestream.write = function (data) {
    //   console.log(data.toString());
    //   return true; // true means 'yes i am ready for more data now'
    //   // OR return false and emit('drain') when ready later
    // };
    // writestream.end = function (data) {
    //   // no more writes after end
    //   // emit "close" (optional)
    // };

    // var connector = http.get(options, function(response) {
    //   response.on('data', function(chunk) {
    //     writestream.write(chunk);
    //   });
    // });

    // this.status = 200;
    // this.type = 'text/event-stream';
    // this.body = pipe(writestream);
    // this.body.pipe(writestream);
  });

  app
    .use(router.routes())
    .use(router.allowedMethods());
};
