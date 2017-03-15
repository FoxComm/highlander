const superagent = require('superagent');
const _ = require('lodash');

const isServer = typeof self === 'undefined';

function addAuthHeaders(headers) {
  headers['JWT'] = process.env.DEMO_AUTH_TOKEN
}

function serialize(data) {
  if (data.toJSON) data = data.toJSON();

  const params = [];
  for (let param in data) {
    if (data.hasOwnProperty(param)) {
      const value = data[param];
      if (value != null) {
        const asString = _.isObject(value) ? JSON.stringify(value) : value;
        params.push(encodeURIComponent(param) + '=' + encodeURIComponent(asString));
      }
    }
  }
  return params.join('&');
}


function request(method, uri, data, options = {}) {
  const isFormData = !isServer && data instanceof FormData;

  let headers = {};

  addAuthHeaders(headers);

  if (!isFormData) {
    headers['Content-Type'] = 'application/json;charset=UTF-8';
  }
  if (options.headers) {
    headers = Object.assign(headers, options.headers);
  }

  // api: http://visionmedia.github.io/superagent/
  const result = superagent[method.toLowerCase()](uri)
    .set(headers);

  if (data) {
    if (method.toUpperCase() === 'GET') {
      const queryString = serialize(data);
      if (queryString) {
        result.query(queryString);
      }
    } else {
      result.send(data);
    }
  }

  let error = null;

  const unauthorizedHandler = options.unauthorizedHandler ? options.unauthorizedHandler : () => {
    window.location.href = '/login';
  };

  const abort = _.bind(result.abort, result);

  const promise = result
    .then(response => {
      if (response.status == 401) {
        unauthorizedHandler(response);
      }
      if (response.status < 200 || response.status >= 300) {
        error = new Error(response.text);
        error.response = response;
      }

      if (error) {
        throw error;
      }

      return response.body;
    });

  // pass through abort method
  promise.abort = abort;
  return promise;
}

class Api {
  static apiURI(uri) {
    uri = uri.replace(/^\/api\/v\d\/|^\//, '');
    uri = `/api/v1/${uri}`;
    if (isServer) {
      uri = `${process.env.API_URL}${uri}`;
    }
    return uri;
  }

  static request(method, uri, data, options = {}) {
    return request(method, this.apiURI(uri), data, options);
  }

  static get(...args) {
    return this.request('GET', ...args);
  }

  static post(...args) {
    return this.request('POST', ...args);
  }

  static delete(...args) {
    return this.request('DELETE', ...args);
  }

  static put(...args) {
    return this.request('PUT', ...args);
  }

  static patch(...args) {
    return this.request('PATCH', ...args);
  }
}

module.exports = Api;



