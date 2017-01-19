/* @flow weak */

import Api from '@foxcomm/api-js';
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

function createApi(options) {
  return new FoxApi({
    stripe_key: (isServer ? process.env : window).STRIPE_PUBLISHABLE_KEY,
    ...options,
  });
}

const api = createApi({
  api_url: isServer ? `${process.env.API_URL}/api` : '/api',
});

export {
  api,
  createApi,
};
