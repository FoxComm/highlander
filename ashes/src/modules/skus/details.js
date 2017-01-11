// @flow

import Api from 'lib/api';
import { createReducer, createAction } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';

export const reset = createAction();
export const clearSubmitErrors = createAction();
export const clearArchiveErrors = createAction();
export const updateSku = createAction();
export const createSku = createAction();
export const archiveSku = createAction();
export const skuNew = createAction();


const _fetchSku = createAsyncActions(
  'fetchSku',
  function(id: number) {
    return Api.get(`/inventory/skus/${id}`);
  }
);

export const fetchSku = _fetchSku.perform;
export const clearFetchErrors = _fetchSku.clearErrors;

const initialState = {
  sku: null,
};

type Dimension = {
  value: number,
  units: string,
}

type QuantityLevel = {
  isEnabled: boolean,
  level: number,
}


export type Sku = {
  id: number,
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

const reducer = createReducer({
  [_fetchSku.succeeded]: (state, sku: Sku) => {
    return {
      ...state,
      sku,
    };
  },
}, initialState);

export default reducer;


