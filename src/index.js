/*
 * @class FoxApi
 * Javascript Library for interacting with FoxCommerce API
 *
 * @example
 *
 * ```js
 * const api = new FoxApi({api_url: 'http://api.foxcommerce'});
 * api.products.search({state: 'cart'});
 * ```
 */

import request from './utils/request';
import setup from './api/index'

class Api {
  constructor(args) {
    // @option api_url: String
    // Required option. Should point to phoenix backend.
    if (!args.api_url) throw new Error('you must specify an API URL');
    this.api_url = args.api_url.replace(/\/?$/, '/'); // ensure trailing slash
  }

  // @section Base methods
  // @method addAuth(jwt: String): FoxApi
  // Set jwt authorization header for next requests
  addAuth(jwt) {
    if (jwt) {
      this.headers = {
        ...this.headers,
        JWT: jwt,
      };
    }
    return this;
  }

  // @method addHeaders(header: Object): FoxApi
  // Set http headers for next requests
  addHeaders(headers) {
    this.headers = headers;
    return this;
  }

  // @method uri(uri: String): String
  // Prepares and returns final url which will be used in request.
  uri(uri) {
    return `${this.api_url}${uri}`
  }

  idOrSlugToArgs(id) {
    return Number.isInteger(id) ? { id: id } : { slug: id }
  }

  queryStringToObject(q) {
    // convoluted 1-liner instead of forloop [probably less performant too, but for loops kill me]
    return q.split('&').reduce(function(acc, n) {
      return n = n.split('='), acc[n[0]] = n[1], acc;
    }, {});
  }

  // Request Methods

  // @method request(method: String, uri: String, data?: Object, options?: Object): Promise
  // Makes http request, possible options:
  // - headers: headers to sent
  request(method, uri, data, options = {}) {
    const finalUrl = this.uri(uri);
    if (this.headers) {
      options.headers = { // eslint-disable-line no-param-reassign
        ...this.headers,
        ...(options.headers || {}),
      };
    }
    return request(method, finalUrl, data, options);
  }

  // @method get(uri: String, data?: Object, options?: Object): Promise
  // Makes GET http request
  get(...args) {
    return this.request('get', ...args);
  }

  // @method post(uri: String, data?: Object, options?: Object): Promise
  // Makes POST http request
  post(...args) {
    return this.request('post', ...args);
  }

  // @method patch(uri: String, data?: Object, options?: Object): Promise
  // Makes PATCH http request
  patch(...args) {
    return this.request('patch', ...args);
  }

  // @method delete(uri: String, data?: Object, options?: Object): Promise
  // Makes DELETE http request
  delete(...args) {
    return this.request('delete', ...args);
  }
}

export default setup(Api);
