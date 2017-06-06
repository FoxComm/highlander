const proxy = require('koa-proxy');
const convert = require('koa-convert');
const PassThrough = require('stream').PassThrough;

// webpack-dev-server serves from 4001 port,
// so, we need to proxy all hmr-related requests
module.exports = function(app) {


  app.use(async function (ctx, next) {
    if (ctx.url !== '/sse/v1/notifications') {
      await next();
      return;
    }

    const stream = new PassThrough();
    const send = (msg) => stream.write(sse('message', msg));

    ctx.req.on('close', () => ctx.res.end());
    ctx.req.on('finish', () => ctx.res.end());
    ctx.req.on('error', () => ctx.res.end());
    ctx.type = 'text/event-stream';
    ctx.body = stream;

    setInterval(() => {
      send({"id":4,"kind":"assigned","data":{"admin":{"id":2,"name":"Frankly Admin","email":"admin@admin.com","disabled":false,"createdAt":"2016-11-22T21:47:22.290Z","isBlacklisted":false},"entity":{"total":10800,"placedAt":"2016-11-30T02:23:21.041Z","orderState":"fulfillmentStarted","shippingState":"fulfillmentStarted","referenceNumber":"BR10303"},"assignees":[{"id":2,"name":"Frankly Admin","email":"admin@admin.com","disabled":false,"createdAt":"2016-11-22T21:47:22.290Z","isBlacklisted":false}],"referenceType":"order","assignmentType":"watcher"},"context":{"userId":2,"userType":"user","transactionId":"9b57a53e-23b9-41b5-b7d0-56e659eee21e","scope":"1"},"createdAt":"2017-06-06T18:13:55.460Z"});
    }, 2000);
  });

  app.use(convert(proxy({
    host: `http://localhost:${process.env.WEBPACK_PORT}`,
    match: /^\/admin/,
  })));
};

const sse = (event, msg) => {
  const data = JSON.stringify(msg);

  return `data: ${data}\n\n`
}
