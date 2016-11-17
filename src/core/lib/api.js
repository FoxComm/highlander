/* @flow weak */

import Api from 'api-js';
import { browserHistory } from 'react-router';
import { authBlockTypes } from 'paragons/auth';

const isServer: boolean = typeof self === 'undefined';

const _unauthorizedHandler = () => {
  if (typeof window != 'undefined') {
    localStorage.removeItem('jwt');
    document.cookie = 'JWT=; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
    browserHistory.push(`?auth=${authBlockTypes.LOGIN}`);
  }
};

class FoxApi extends Api {
  foxRequest(method, uri, data, options = {}) {
    const patchedHeaders = {
      ...(options.headers),
      'Cache-control': 'no-cache',
      Pragma: 'no-cache',
      Expires: 0,
    };
    const requestOpts = { ...options, headers: patchedHeaders, unauthorizedHandler: _unauthorizedHandler };
    return this.request(method, uri, data, requestOpts);
  }

  get(...args) {
    return this.foxRequest('get', ...args);
  }

  post(...args) {
    return this.foxRequest('post', ...args);
  }

  patch(...args) {
    return this.foxRequest('patch', ...args);
  }

  delete(...args) {
    return this.foxRequest('delete', ...args);
  }
}

const api = new FoxApi({
  api_url: isServer ? `${process.env.API_URL}/api` : '/api',
  stripe_key: process.env.STRIPE_PUBLISHABLE_KEY,
});

export { api };
