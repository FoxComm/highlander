/**
 * @flow
 */

import Api from 'lib/api';
import { assoc, dissoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import createAsyncActions from '../async-utils';
import _ from 'lodash';

import { createEmptySku } from 'paragons/sku';

import { pushStockItemChanges } from '../inventory/warehouses';

export type Sku = {
  code?: string,
  feCode?: string,
  attributes: Attributes,
  id: any,
  context: {
    attributes?: Object,
    name: string,
  }
};

const defaultContext = 'default';

export const skuNew = createAction('SKU_NEW');
const skuClear = createAction('SKU_CLEAR');

const _archiveSku = createAsyncActions(
  'archiveSku',
  (code, context = defaultContext) => {
    return Api.delete(`/skus/${context}/${code}`);
  }
);

export const archiveSku = _archiveSku.perform;

const _fetchSku = createAsyncActions(
  'fetchSku',
  (code: string, context: string = defaultContext) => {
    return Api.get(`/skus/${context}/${code}`);
  }
);

const _createSku = createAsyncActions(
  'createSku',
  (sku: Sku, context: string = defaultContext) => {
    return Api.post(`/skus/${context}`, sku);
  }
);

const _updateSku = createAsyncActions(
  'updateSku',
  function(sku: Sku, context: string = defaultContext) {
    const { dispatch, getState } = this;
    const oldSku = _.get(getState(), ['skus', 'details', 'sku', 'attributes', 'code', 'v']);
    if (oldSku) {
      const stockItemsPromise = dispatch(pushStockItemChanges(oldSku));
      const updatePromise = Api.patch(`/skus/${context}/${oldSku}`, sku);
      return Promise.all([updatePromise, stockItemsPromise]).then(([updateResponse]) => {
        return updateResponse;
      });
    }
  }
);

export const fetchSku = _fetchSku.perform;
export const createSku = _createSku.perform;
export const updateSku = _updateSku.perform;

export function clearSubmitErrors() {
  return (dispatch: Function) => {
    dispatch(_createSku.clearErrors());
    dispatch(_updateSku.clearErrors());
  };
}

export const clearFetchErrors = _fetchSku.clearErrors;

export function reset() {
  return (dispatch: Function) => {
    dispatch(skuClear());
    dispatch(clearSubmitErrors());
    dispatch(clearFetchErrors());
  };
}

export type SkuState = {
  sku: ?Sku,
}

const initialState: SkuState = {
  sku: null,
};

function updateSkuInState(state: SkuState, sku: Sku) {
  return {
    ...state,
    sku,
  };
}

const reducer = createReducer({
  [skuNew]: (state) => {
    return assoc(state,
      'sku', createEmptySku(),
      'err', null
    );
  },
  [skuClear]: state => {
    return dissoc(state, 'sku');
  },
  [_createSku.succeeded]: updateSkuInState,
  [_updateSku.succeeded]: updateSkuInState,
  [_fetchSku.succeeded]: updateSkuInState,
}, initialState);

export default reducer;
