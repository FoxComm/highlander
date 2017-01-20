/* @flow weak */

import Api from 'api-js';
const isServer: boolean = typeof self === 'undefined';

export const api = new Api({
  api_url: isServer ? `${process.env.API_URL}/api` : '/api',
  stripe_key: (isServer ? process.env : window).STRIPE_PUBLISHABLE_KEY,
});
