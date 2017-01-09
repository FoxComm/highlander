/**
 * @flow
 */

import Api from 'lib/api';
import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';

const _fetchSku = createAsyncActions(
  'fetchSku',
  function(id: number) {
    return Api.get(`/inventory/skus/${id}`);
  }
);

export const fetchSku = _fetchSku.perform;

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


