/**
 * @flow
 */

import { createAction, createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import Api from 'lib/api';
import { getUserId } from 'lib/claims';

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

export function clearSubmitErrors() {
  return (dispatch: Function) => {
    dispatch(_createSku.clearErrors());
    dispatch(_updateSku.clearErrors());
  };
}

const _fetchSuggest = createAsyncActions(
  'fetchSuggest',
  (product_id: string, text: string) => {
    return Api.get(`/amazon/categories/suggester?product_id=${product_id}&q=${text}`);
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
    return Api.get(`/amazon/categories/schema?category_id=${category_id}`);
  }
);

export const fetchAmazonSchema = _fetchAmazonSchema.perform;

const _fetchAmazonCredentials = createAsyncActions(
  'fetchAmazonCredentials',
  // @todo move customer_id (which now emulated through getUserId) to hyperion
  () => {
    const userId = getUserId() || '';

    return Api.get(`/amazon/credentials/${userId}`);
  }
);

export const fetchAmazonCredentials = _fetchAmazonCredentials.perform;

const _updateAmazonCredentials = createAsyncActions(
  'updateAmazonCredentials',
  (params) => Api.post(`/amazon/credentials`, params)
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
