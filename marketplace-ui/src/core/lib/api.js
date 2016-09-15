import axios from 'axios';

export class Api {
  defaultHeaders = {
    'Content-Type': 'application/json;charset=UTF-8',
  };

  constructor(args) {
    if (!args.baseUrl) throw new Error('You must specify an API URL');

    this.baseUrl = args.baseUrl.replace(/\/?$/, '');
  }

  request(method, url, data, options = {}) {
    return axios({
      method,
      url,
      data,
      baseURL: this.baseUrl,
      headers: {
        ...this.defaultHeaders,
        ...(options && options.headers || {}),
      },
    });
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

const isServer: boolean = typeof self === 'undefined';

export default new Api({
  baseUrl: isServer ? `${process.env.API_URL}/api` : '/api',
  stripe_key: process.env.STRIPE_PUBLISHABLE_KEY,
});
