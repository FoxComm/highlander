import request from './utils/request';
import setup from './api/index'

const MAX_RESULTS = 1000

class Api {
  constructor(args) {
    if (!args.api_url) throw new Error('you must specify an API URL');
    this.api_url = args.api_url.replace(/\/?$/, '/'); // ensure trailing slash
    this.version = args.version || 'v1';
    this.path = {
      login: '/public/login',
      signup: '/public/registrations/new',
      search: `/search/products_catalog_view/_search?size=${MAX_RESULTS}`,
      addToCart: '/my/cart/add',
      updateQty: '/my/cart/line-items',
      removeFromCart: '/my/cart/line-items/:id/edit',
    }
  }

  // Headers

  addAuth(jwt) {
    this.headers = {
      ...this.headers,
      JWT: jwt,
    };
    return this;
  }

  addHeaders(headers) {
    this.headers = headers;
    return this;
  }

  // Utils

  uri(uri) {
    return `${this.api_url}${this.version}${uri}`
  }

  idOrSlugToArgs(id) {
    return Number.isInteger(id) ? { id: id } : { slug: id }
  }

  queryStringToObject(q) {
    // convoluted 1-liner instead of forloop [probably less performant too, but for loops kill me]
    return q.split('&').map(function(n){ return n = n.split('='), this[n[0]] = n[1], this }.bind({}))[0]
  }

  // Request Methods

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

  get(...args) {
    return this.request('get', ...args);
  }

  post(...args) {
    return this.request('post', ...args);
  }

  patch(...args) {
    return this.request('patch', ...args);
  }

  delete(...args) {
    return this.request('delete', ...args);
  }

}

export default setup(Api);
