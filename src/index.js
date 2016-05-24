
import request from './utils/request';
import setup from './api/index'

class Api {
  constructor(args) {
    if (!args.api_url) throw new Error('you must specify an API URL');
    this.api_url = args.api_url.replace(/\/?$/, '/'); // ensure trailing slash
    this.version = args.version || 'v1';
  }

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

  uri(uri) {
    return `${this.api_url}${this.version}${uri}`
  }

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
// export default Api;
export default setup(Api);
