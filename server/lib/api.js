const _ = require('lodash');
const request = require('request');

class Api {
  constructor(uri, auth) {
    this.baseRequest = request.defaults({
      baseUrl: uri,
      auth: auth,
      _json: true
    });
  }

  request(method, uri, data, token) {
    let _this = this;
    if (_(data).isString()) {
      token = data;
      data = undefined;
    }

    return new Promise(function(resolve, reject) {
      let opts = {
        uri: uri,
        method: method,
        headers: {
          accept: 'application/json',
          authorization: token ? `Bearer ${token}` : ''
        }
      };

      if (!_.isEmpty(data)) {
        opts[(method === 'GET' ? 'qs' : 'json')] = data;
      }
      _this.baseRequest(opts, function(err, r, body) {
        if (err) return reject(err);

        let response = {
          status: r.statusCode,
          response: body
        };
        if (r.statusCode >= 200 && r.statusCode < 300) {
          resolve(response);
        } else {
          reject(response);
        }
      });
    });
  }

  buildRequest(method, args) {
    args = _.toArray(args);
    return this.request.apply(this, [method].concat(args));
  }

  get() { return this.buildRequest('GET', arguments); }
  post() { return this.buildRequest('POST', arguments); }
  put() { return this.buildRequest('PUT', arguments); }
  patch() { return this.buildRequest('PATCH', arguments); }
  delete() { return this.buildRequest('DELETE', arguments); }
}

module.exports = Api;
