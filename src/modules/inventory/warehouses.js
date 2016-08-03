// @flow weak

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import createAsyncActions from '../async-utils';

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
      "sku": "SKU-SKU",
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
      "sku": "SKU-SKU",
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
  (skuCode, qty) => {
    let payload, action;
    if (qty >= 0) {
      payload = {Qty: qty};
      action = 'increment';
    } else {
      payload = {Qty: -qty};
      action = 'decrement';
    }
    return Api.patch(`/inventory/stock-items/${skuCode}/${action}`, payload);
  }
);

export const changeItemUnits = _changeItemUnits.perform;

const initialState = {};

const reducer = createReducer({
  [_fetchSummary.succeeded]: (state, [payload, sku]) => {
    const inventoryDetails: Array<InventorySummary> = payload;

    return {
      ...state,
      [sku]: inventoryDetails,
    };
  },
}, initialState);

export default reducer;
