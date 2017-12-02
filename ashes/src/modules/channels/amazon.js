/* @flow */

import { createAction, createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcommerce/wings';
import Api from 'lib/api';
import schemaFixture from './schema.fixture';

const initialState = {
  credentials: null,
  suggest: {
    primary: null,
    secondary: [],
  },
  push: null,
  productStatus: null,
};

export function clearErrors() {
  return (dispatch: Function) => {
    dispatch(_fetchSuggest.clearErrors());
    dispatch(_fetchAmazonSchema.clearErrors());
    dispatch(_fetchAmazonCredentials.clearErrors());
    dispatch(_updateAmazonCredentials.clearErrors());
  };
}

export const resetState = createAction('RESET_AMAZON_STATE');

const _fetchAmazonStatus = createAsyncActions(
  'fetchAmazonStatus',
  () => Api.get(`/hyperion/credentials/status`)
);

export const fetchAmazonStatus = _fetchAmazonStatus.perform;

const _fetchAmazonCredentials = createAsyncActions(
  'fetchAmazonCredentials',
  () => Api.get(`/hyperion/credentials/`)
);

export const fetchAmazonCredentials = _fetchAmazonCredentials.perform;

const _updateAmazonCredentials = createAsyncActions(
  'updateAmazonCredentials',
  (params) => {
    const data = {
      seller_id: params.seller_id,
      mws_auth_token: params.mws_auth_token,
    };

    return Api.post(`/hyperion/credentials/`, data);
  }
);

export const updateAmazonCredentials = _updateAmazonCredentials.perform;

const _removeAmazonCredentials = createAsyncActions(
  'removeAmazonCredentials',
  () => Api.delete(`/hyperion/credentials/`)
);

export const removeAmazonCredentials = _removeAmazonCredentials.perform;

const _fetchSuggest = createAsyncActions(
  'fetchSuggest',
  (title: string, q: string) => {
    const data = { q, title };

    return Api.get(`/hyperion/categories/suggest`, data);
  }
);

export const fetchSuggest = _fetchSuggest.perform;

const _fetchAmazonSchema = createAsyncActions(
  'fetchAmazonSchema',
  (category_id: string) => {
    // @todo move to hyperion
    // return Api.get(`/amazon/categories/schema?category_id=${category_id}`);
    return new Promise(function(resolve, reject) {
      setTimeout(() => resolve(schemaFixture), 1000);
    });
  }
);

export const fetchAmazonSchema = _fetchAmazonSchema.perform;

const _pushToAmazon = createAsyncActions(
  'pushToAmazon',
  (id) => {
    // https://github.com/FoxComm/highlander/blob/amazon/engineering-wiki/hyperion/README.md#push-product-to-amazon
    const data = {
      purge: true
    };

    return Api.post(`/hyperion/products/${id}/push`, data);
  }
);

export const pushToAmazon = _pushToAmazon.perform;

const _fetchAmazonProductStatus = createAsyncActions(
  'fetchAmazonProductStatus',
  (id) => {
    const data = {};

    return Api.get(`/hyperion/products/${id}/result`, data);
  }
);

export const fetchAmazonProductStatus = _fetchAmazonProductStatus.perform;

const reducer = createReducer({
  [_fetchSuggest.succeeded]: (state, res) => ({ ...state, suggest: res }),
  [_fetchAmazonSchema.succeeded]: (state, res) => ({ ...state, schema: res }),

  [_fetchAmazonStatus.succeeded]: (state, res) => ({ ...state, status: res.credentials }),
  [_fetchAmazonCredentials.succeeded]: (state, res) => ({ ...state, credentials: res }),
  [_updateAmazonCredentials.succeeded]: (state, res) => ({ ...state, credentials: res }),
  [_removeAmazonCredentials.succeeded]: (state, res) => ({ ...state, credentials: null }),

  [_pushToAmazon.succeeded]: (state, res) => ({ ...state, push: res }),
  [_fetchAmazonProductStatus.succeeded]: (state, res) => ({ ...state, productStatus: res }),

  [resetState]: (state) => initialState,
}, initialState);

export default reducer;
