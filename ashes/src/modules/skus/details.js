// @flow

import Api from 'lib/api';
import { createReducer, createAction } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';

import { pushStockItemChanges } from 'modules/inventory/warehouses';

export const reset = createAction();
export const clearSubmitErrors = createAction();
export const clearArchiveErrors = createAction();
export const createSku = createAction();
export const archiveSku = createAction();
export const skuNew = createAction();

type Dimension = {
  value: number,
  units: string,
}

type QuantityLevel = {
  isEnabled: boolean,
  level: number,
}

type SkuBase = {
  code: string,
  upc: string,
  title: string,
  unitCost: Currency,
  taxClass: string,
  requiresShipping: boolean,
  shippingClass: string,
  isReturnable: boolean,
  returnWindow: Dimension,
  width: Dimension,
  height: Dimension,
  weight: Dimension,
  length: Dimension,
  requiresInventoryTracking: boolean,
  inventoryWarningLevel: QuantityLevel,
  maximumQuantityInCart: QuantityLevel,
  minimumQuantityInCart: QuantityLevel,
  allowBackorder: boolean,
  allowPreorder: boolean,
  requiresLotTracking: boolean,
  lotExpirationThreshold: Dimension,
  lotExpirationWarningThreshold: Dimension,
}


export type Sku = SkuBase & {
  id: number,
}

const _fetchSku = createAsyncActions(
  'fetchSku',
  function(id: number) {
    const r = require('./tmp-sku-mock');
    return new Promise(resolve => resolve(r));
    //return Api.get(`/inventory/skus/${id}`);
  }
);

export const fetchSku = _fetchSku.perform;
export const clearFetchErrors = _fetchSku.clearErrors;

const _updateSku = createAsyncActions(
  'updateSku',
  function(sku: Sku) {
    const {id, ...payload} = sku;
    const willUpdateObject = Api.patch(`/inventory/skus/${id}`, payload);
    const willUpdateStockCounts = pushStockItemChanges(id);

    return Promise.all([willUpdateObject, willUpdateStockCounts])
      .then(responses => responses[0]);
  }
);

export const updateSku = _updateSku.perform;


const initialState = {
  sku: null,
};

function updateSkuInState(state, sku: Sku) {
  return {
    ...state,
    sku,
  };
}

const reducer = createReducer({
  [_fetchSku.succeeded]: updateSkuInState,
  [_updateSku.succeeded]: updateSkuInState,
}, initialState);

export default reducer;


