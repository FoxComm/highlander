'use strict';

const
  _       = require('underscore'),
  request = require('request').defaults({_json: true});

module.exports = {
  request: function(method, uri, data, token) {
    if (_(data).isString()) {
      token = data;
      data  = undefined;
    }

    return function(fn) {
      uri = /^https?:/.test(uri) ? uri : `http://localhost:3001${uri}`;

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

      return request(opts, fn);
    }
  },

  get: function() {
    let args = [].slice.call(arguments);
    return this.request.apply(this, ['GET'].concat(args));
  },

  post: function() {
    let args = [].slice.call(arguments);
    return this.request.apply(this, ['POST'].concat(args));
  },

  put: function() {
    let args = [].slice.call(arguments);
    return this.request.apply(this, ['PUT'].concat(args));
  },

  delete: function() {
    let args = [].slice.call(arguments);
    return this.request.apply(this, ['DELETE'].concat(args));
  }

}
