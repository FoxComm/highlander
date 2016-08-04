// @flow weak

import _ from 'lodash';
import { assoc } from 'sprout-data';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import createAsyncActions from '../async-utils';

export const updateSkuItemsCount = createAction(
  'SKU_UPDATE_ITEMS_COUNT',
  (sku: string, stockItem: StockItem, qty: number) => [sku, stockItem, qty]
);

export type StockCounts = {
  onHand: number,
  onHold: number,
  reserved: number,
  shipped: number,
  afs: number,
  afsCost: number,
}

export type StockLocation = StockCounts & {
  stockLocationId: number,
  stockLocationName: string,
}

export type StockItem = StockCounts & {
  stockItemId: number,
  sku: string,
  type: string,
}

export type InventorySummary = {
  stockLocation: StockLocation,
  stockItems: Array<StockItem>
}

const mockData = [{
  "stockLocation": {
    "stockLocationId": 1,
    "stockLocationName": "Warehouse name",
    "onHand": 1,
    "onHold": 1,
    "reserved": 1,
    "shipped": 1,
    "afs": 1,
    "afsCost": 1
  },
  "stockItems": [
    {
      "stockItemId": 1,
      "sku": "SKU-TRL",
      "type": "Sellable",
      "onHand": 1,
      "onHold": 1,
      "reserved": 1,
      "shipped": 1,
      "afs": 1,
      "afsCost": 1
    },
    {
      "stockItemId": 2,
      "sku": "SKU-TRL",
      "type": "Non-sellable",
      "onHand": 1,
      "onHold": 1,
      "reserved": 1,
      "shipped": 1,
      "afs": 1,
      "afsCost": 1
    }
  ]
}];

// @TODO: get rid of mock data when api will be ready

const _fetchSummary = createAsyncActions(
  'inventory-summary',
  //(skuCode) => Api.get(`/inventory/summary/${skuCode}`),
  (skuCode) => {
    return new Promise(resolve => resolve(mockData));
  },
  (...args) => [...args]
);
export const fetchSummary = _fetchSummary.perform;

const _changeItemUnits = createAsyncActions(
  'inventory-increment',
  (stockItemId: number, qty: number, type: string) => {
    let payload, action;
    if (qty >= 0) {
      payload = {Qty: qty};
      action = 'increment';
    } else {
      payload = {Qty: -qty};
      action = 'decrement';
    }
    payload.type = type;
    return Api.patch(`/inventory/stock-items/${stockItemId}/${action}`, payload);
  }
);

export const changeItemUnits = _changeItemUnits.perform;

export function pushStockItemChanges(sku) {
  return (dispatch, getState) => {
    const stockItemChanges = _.get(getState(), ['inventory', 'warehouses', 'stockItemChanges', sku]);

    if (stockItemChanges) {
      const promises = _.map(stockItemChanges, (payload: Object, stockItemId: number) => {
        return dispatch(changeItemUnits(stockItemId, payload.diff, payload.type));
      });

      return Promise.all(promises);
    }
  };
}

const initialState = {};

const reducer = createReducer({
  [_fetchSummary.succeeded]: (state, [payload, sku]) => {
    const inventoryDetails: Array<InventorySummary> = payload;

    return {
      ...state,
      [sku]: inventoryDetails,
    };
  },
  [updateSkuItemsCount]: (state: SkuState, [sku, stockItem, diff]) => {
    return assoc(state,
      ['stockItemChanges', sku, stockItem.stockItemId], {diff, type: stockItem.type}
    );
  },
}, initialState);

export default reducer;
