import superagent from 'superagent';
import _ from 'lodash';
import { Tracer, ExplicitContext, BatchRecorder } from '@foxcomm/zipkin';
import { HttpLogger } from '@foxcomm/zipkin-transport-http';

import zipkin from '../opt/superagent-zipkin';

const isServer = typeof self === 'undefined';

export function appendQueryString(url, queryString) {
  if (!queryString) {
    return url;
  }
  const joinWith = url.indexOf('?') != -1 ? '&' : '?';

  return `${url}${joinWith}${queryString}`;
}

export function addAuthHeaders(headers) {
  const token = localStorage.getItem('jwt');

  if (isServer) {
    const demoToken = process.env.DEMO_AUTH_TOKEN;
    if (demoToken) headers['Authorization'] = `Basic ${demoToken}`;
    return;
  }

  if (token) headers['JWT'] = token;
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


export function request(method, uri, data, options = {}) {
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

  if (!isServer && process.env.NODE_ENV !== 'test') {
    const recorder = new BatchRecorder({
      logger: new HttpLogger({
        endpoint: window.ZIPKIN_URL,
      })
    });

    const ctxImpl = new ExplicitContext();
    const tracer = new Tracer({ recorder, ctxImpl });

    result.use(zipkin(tracer)('ashes'));
  }

  const promise = result
    .then(
      response => response.body,
      err => {
        if (err.statusCode == 401) {
          unauthorizedHandler(err.response);
        }

        error = new Error(_.get(err, 'message', String(err)));
        error.response = err.response;

        throw error;
      });

  // pass through abort method
  promise.abort = abort;
  return promise;
}

export default class Api {
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

  static submitForm(form) {
    const method = form.getAttribute('method');
    const uri = form.getAttribute('action');

    return this.request(method, uri, new FormData(form));
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


