/**
 * @flow
 */

import { createAction, createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import Api, { request } from 'lib/api';
import { getUserId } from 'lib/claims';
import schemaFixture from './schema.fixture';

const initialState = {
  credentials: null,
  suggest: {
    primary: null,
    secondary: [],
  },
};

export function clearErrors() {
  return (dispatch: Function) => {
    dispatch(_fetchSuggest.clearErrors());
    dispatch(_fetchAmazonCategory.clearErrors());
    dispatch(_fetchAmazonSchema.clearErrors());
    dispatch(_fetchAmazonCredentials.clearErrors());
    dispatch(_updateAmazonCredentials.clearErrors());
  };
}

const _fetchSuggest = createAsyncActions(
  'fetchSuggest',
  (title: string, q: string) => {
    const userId = getUserId() || '';
    const data = { title, q };
    // @todo move to hyperion
    const options = {
      headers: {
        'Customer-ID': userId,
      },
    };

    return Api.get(`/hyperion/categories/suggest`, data, options);
  }
);

export const fetchSuggest = _fetchSuggest.perform;

// @TODO
const _fetchAmazonCategory = createAsyncActions(
  'fetchAmazonCategory',
  (product_id: string, text: string) => {
    // return Api.get(`/products/${context}/${id}`);
    return new Promise(function(resolve, reject) {
      setTimeout(() => resolve(1), 1000);
    });
  }
);

export function fetchAmazonCategory(product_id: string, text: string) {
  return (dispatch: Function) => {
    return dispatch(_fetchAmazonCategory.perform(product_id, text));
  };
}

const _fetchAmazonSchema = createAsyncActions(
  'fetchAmazonSchema',
  (category_id: string, text: string) => {
    // @todo move to hyperion
    // return Api.get(`/amazon/categories/schema?category_id=${category_id}`);
    return new Promise(function(resolve, reject) {
      setTimeout(() => resolve(schemaFixture), 1000);
    });
  }
);

export const fetchAmazonSchema = _fetchAmazonSchema.perform;

const _fetchAmazonCredentials = createAsyncActions(
  'fetchAmazonCredentials',
  // @todo move customer_id (which now emulated through getUserId) to hyperion
  () => {
    const userId = getUserId() || '';

    return Api.get(`/hyperion/credentials/${userId}`);
  }
);

export const fetchAmazonCredentials = _fetchAmazonCredentials.perform;

const _updateAmazonCredentials = createAsyncActions(
  'updateAmazonCredentials',
  (params) => {
    const userId = getUserId() || '';
    const data = {
      seller_id: params.seller_id,
      mws_auth_token: params.mws_auth_token,
      client_id: userId,
    };
    // @todo move customer_id (which now emulated through getUserId) to hyperion
    const options = {
      headers: {
        'Customer-ID': userId,
      },
    };

    return Api.post(`/hyperion/credentials/`, data, options);
  }
);

export const updateAmazonCredentials = _updateAmazonCredentials.perform;

const reducer = createReducer({
  [_fetchSuggest.succeeded]: (state, res) => ({ ...state, suggest: res }),
  [_fetchAmazonCategory.started]: (state) => ({ ...state, fields: [] }),
  [_fetchAmazonCategory.succeeded]: (state, res) => ({ ...state, fields: res }),
  [_fetchAmazonSchema.succeeded]: (state, res) => ({ ...state, schema: res }),
  [_fetchAmazonCredentials.succeeded]: (state, res) => ({ ...state, credentials: res }),
  [_updateAmazonCredentials.succeeded]: (state, res) => ({ ...state, credentials: res }),
}, initialState);

export default reducer;
