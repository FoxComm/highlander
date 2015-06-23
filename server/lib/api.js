'use strict';

const
  _       = require('underscore'),
  request = require('request');

class Api {
  constructor(uri) {
    this.baseRequest = request.defaults({
      baseUrl: uri,
      _json: true
    });
  }

  request(method, uri, data, token) {
    let _this = this;
    if (_(data).isString()) {
      token = data;
      data  = undefined;
    }

    return new Promise(function(resolve, reject) {
      let opts = {
        uri:    uri,
        method: method,
        headers: {
          accept: 'application/json;q=0.9,*/*;q=0.8;',
          authorization: token ? `Bearer ${token}` : ''
        }
      }

      if (data) {
        opts[(method === 'GET' ? 'qs' : 'json')] = data;
      }
      _this.baseRequest(opts, function(e, r, body) {
        let data = {
          status: r.statusCode,
          response: body
        };
        if (r.statusCode >= 200 && r.statusCode < 300) {
          resolve(data);
        } else {
          reject(data);
        }
      });
    });
  }

  buildRequest(method, args) {
    args = [].slice.call(args);
    return this.request.apply(this, [method].concat(args));
  }

  get() { return this.buildRequest('GET', arguments); }
  post() { return this.buildRequest('POST', arguments); }
  put() { return this.buildRequest('PUT', arguments); }
  delete() { return this.buildRequest('DELETE', arguments); }
}

module.exports = Api;
