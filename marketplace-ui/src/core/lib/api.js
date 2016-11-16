import localStorage from 'localStorage';

import request from './request';

export function addAuthHeaders(headers) {
  const token = localStorage.getItem('jwt');

  if (token) {
    return { ...headers, JWT: token };
  }

  return headers;
}

export class Api {
  defaultHeaders = {
    'Content-Type': 'application/json;charset=UTF-8',
  };

  constructor(args) {
    if (!args.baseUrl) throw new Error('You must specify an API URL');

    this.baseUrl = args.baseUrl.replace(/\/?$/, '');
  }

  request(method, url, data, options = {}) {
    const clearUrl = url.replace(/^\//, '');

    const headers = addAuthHeaders(options && options.headers || {});

    return request(method, `${this.baseUrl}/${clearUrl}`, data, {
      headers: {
        ...this.defaultHeaders,
        ...headers,
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
  baseUrl: isServer ? `${process.env.API_URL}` : '/api/v1/mkt',
  stripe_key: process.env.STRIPE_PUBLISHABLE_KEY,
});
