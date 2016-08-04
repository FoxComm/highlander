// @flow weak

import _ from 'lodash';
import { assoc } from 'sprout-data';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import createAsyncActions from '../async-utils';

export const updateSkuItemsCount = createAction(
  'SKU_UPDATE_ITEMS_COUNT',
  (sku, stockItem, qty) => [sku, stockItem, qty]
);

export type StockCounts = {
  onHand: number,
  onHold: number,
  reserved: number,
  shipped: number,
  afs: number,
  afsCost: number,
}

export type StockLocation = {
  stockLocationId: number,
  stockLocationName: string,
}

export type StockItem = {
  stockItemId: number,
  sku: string,
  type: string,
  defaultUnitCost: number,
}

export type StockItemSummary = StockCounts & {
  stockLocation: StockLocation,
  stockItem: StockItem,
}

export type StockItemFlat = StockCounts & StockItem;

export type WarehouseInventorySummary = StockCounts & {
  stockLocation: StockLocation,
  stockItems: Array<StockItemFlat>,
}

export type WarehouseInventoryMap = {
  [stockLocationId: number]: WarehouseInventorySummary
}

// @TODO: now you can test PR with this data if backend will be not available
/*
const mockData = [{
  "stockLocation": {
    "stockLocationId": 1,
    "stockLocationName": "Warehouse name",
  },
  "stockItem": {
    "stockItemId": 1,
    "sku": "SKU-TRL",
    "type": "Sellable",
    "defaultUnitCost": 20,
  },
  "onHand": 1,
  "onHold": 1,
  "reserved": 1,
  "shipped": 1,
  "afs": 1,
  "afsCost": 1
}, {
  "stockLocation": {
    "stockLocationId": 1,
    "stockLocationName": "Warehouse name",
  },
  "stockItem": {
    "stockItemId": 2,
    "sku": "SKU-TRL",
    "type": "Non-sellable",
    "defaultUnitCost": 24,
  },
  "onHand": 1,
  "onHold": 1,
  "reserved": 1,
  "shipped": 1,
  "afs": 1,
  "afsCost": 1
}]; */

const _fetchSummary = createAsyncActions(
  'inventory-summary',
  (skuCode) => Api.get(`/inventory/summary/${skuCode}`),
  (...args) => [...args]
);
export const fetchSummary = _fetchSummary.perform;

const _changeItemUnits = createAsyncActions(
  'inventory-increment',
  (stockItemId: number, qty: number, type: string) => {
    let payload, action;
    if (qty >= 0) {
      payload = {qty: qty, type};
      action = 'increment';
    } else {
      payload = {qty: -qty, type};
      action = 'decrement';
    }
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
    const stockItems: Array<StockItemSummary> = payload;

    const inventoryDetailsByLocations = _.reduce(stockItems, (acc, itemSummary: StockItemSummary) => {
      const warehouseDetails =
        acc[itemSummary.stockLocation.stockLocationId] = acc[itemSummary.stockLocation.stockLocationId] || {
        stockItems: [],
        stockLocation: itemSummary.stockLocation,
        onHand: 0,
        onHold: 0,
        reserved: 0,
        shipped: 0,
        afs: 0,
        afsCost: 0,
      };

      warehouseDetails.onHand += itemSummary.onHand;
      warehouseDetails.onHold += itemSummary.onHold;
      warehouseDetails.reserved += itemSummary.reserved;
      warehouseDetails.shipped += itemSummary.shipped;
      warehouseDetails.afs += itemSummary.afs;
      warehouseDetails.afsCost += itemSummary.afsCost;

      const stockItemsCounts = _.omit(itemSummary, ['stockLocation', 'stockItem']);

      warehouseDetails.stockItems.push({
        ...stockItemsCounts,
        ...itemSummary.stockItem,
      });

      return acc;
    }, {});

    return assoc(state,
      ['details', sku], inventoryDetailsByLocations
    );
  },
  [updateSkuItemsCount]: (state, [sku, stockItem, diff]) => {
    return assoc(state,
      ['stockItemChanges', sku, stockItem.stockItemId], {diff, type: stockItem.type}
    );
  },
}, initialState);

export default reducer;
