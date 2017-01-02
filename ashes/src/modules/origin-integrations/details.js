/* @flow */

import { createReducer } from 'redux-act';
import Api from 'lib/api';
import { createAsyncActions } from '@foxcomm/wings';

import type { OriginIntegration, ProductFeed } from 'paragons/origin-integration';

type State = {
  originIntegration: ?OriginIntegration,
  productFeed: ?ProductFeed,
};

const initialState = {
  originIntegration: null,
  productFeed: null,
};

const _getOriginIntegration = createAsyncActions(
  'getOriginIntegration',
  (id: number) => Api.get(`/mkt/users/${id}/origin_integrations`)
);

const _createOriginIntegration = createAsyncActions(
  'createOriginIntegrtion',
  (id: number, data: OriginIntegration) => Api.post(`/mkt/users/${id}/origin_integrations`, data)
);

const _updateOriginIntegration = createAsyncActions(
  'updateOriginIntegration',
  (id: number, data: OriginIntegration) => Api.patch(`/mkt/users/${id}/origin_integrations`, data)
);

const _getProductFeed = createAsyncActions(
  'getProductFeed',
  (id: number) => Api.get(`/mkt/users/${id}/product_feed`)
);

const _createProductFeed = createAsyncActions(
  'createProductFeed',
  (id: number, data: ProductFeed) => Api.post(`/mkt/users/${id}/product_feed`, data)
);

const _updateProductFeed = createAsyncActions(
  'updateProductFeed',
  (id: number, data: ProductFeed) => Api.patch(`/mkt/users/${id}/product_feed`, data)
);

export const fetchOriginIntegration = _getOriginIntegration.perform;
export const createOriginIntegration = _createOriginIntegration.perform;
export const updateOriginIntegration = _updateOriginIntegration.perform;
export const fetchProductFeed = _getProductFeed.perform;
export const createProductFeed = _createProductFeed.perform;
export const updateProductFeed = _updateProductFeed.perform;

function originIntegrationSucceeded(state: State, payload: Object): State {
  const originIntegration = payload.origin_integration || payload;
  return { ...state, originIntegration };
}

function productFeedSucceeded(state: State, payload: Object): State {
  const productFeed = payload.product_feed || payload;
  return { ...state, productFeed };
}

const reducer = createReducer({
  [_getOriginIntegration.succeeded]: originIntegrationSucceeded,
  [_createOriginIntegration.succeeded]: originIntegrationSucceeded,
  [_updateOriginIntegration.succeeded]: originIntegrationSucceeded,
  [_getProductFeed.succeeded]: productFeedSucceeded,
  [_createProductFeed.succeeded]: productFeedSucceeded,
  [_updateProductFeed.succeeded]: productFeedSucceeded,
}, initialState);

export default reducer;
