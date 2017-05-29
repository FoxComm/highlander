const request = require('co-request');
const Cache = require('./cache');
const send = require('koa-sendfile');

function proxy(options) {
  const cache = new Cache(options.cacheDir || '/tmp');

  return function* () {
    const self = this;
    const req = this.request;

    if (yield cache.replacementExists(req)) {
      const replPath = cache.replacementPath(req);

      yield send(this, replPath);
      console.log(`HTTP ${req.method} ${self.url} (overrided by ${replPath})`.yellow);
    } else if (yield cache.exists(req)) {
      console.log(`HTTP ${req.method} ${self.url} (from cache)`.green);
      yield cache.applyCache(req, this);
    } else {
      const requestOpts = {
        url: options.host + this.url,
        method: this.method,
        headers: Object.assign({}, this.headers || {}),
        qs: this.query,
        encoding: null
      };
      // patch accept-encoding header in order to get non-encoded response
      requestOpts.headers['accept-encoding'] = 'identity';

      // something possibly went wrong if they have no body but are sending a
      // put or a post
      if ((requestOpts.method == 'POST' || requestOpts.method == 'PUT')) {

        if (!this.request.fields && !this.request.body) {
          console.warn('sending PUT or POST but no request body found');
        } else {
          requestOpts.body = this.request.body || JSON.stringify(this.request.fields) ;
        }
      }

      console.log(`HTTP ${requestOpts.method} ${requestOpts.url} (cache miss)`);

      const response = yield request(requestOpts);
      if (response.statusCode >= 200 && response.statusCode < 400) {
        yield cache.writeMeta(req, response);
        yield cache.writeData(req, response);
      }

      Object.keys(response.headers).map(name => {
        if (name != 'transfer-encoding') {
          self.set(name, response.headers[name]);
        }
      });
      this.status = response.statusCode;
      this.body = response.body;
    }
  }
}


module.exports = proxy;