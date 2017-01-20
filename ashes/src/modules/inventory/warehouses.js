// @flow weak

import _ from 'lodash';
import { assoc } from 'sprout-data';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';

export const updateSkuItemsCount = createAction(
  'SKU_UPDATE_ITEMS_COUNT',
  (sku, stockItem, qty) => [sku, stockItem, qty]
);

const clearSkuItemsChanges = createAction('SKU_CLEAR_ITEMS_CHANGES');

export type StockCounts = {
  onHand: number,
  onHold: number,
  reserved: number,
  shipped: number,
  afs: number,
  afsCost: number,
}

export type StockLocation = {
  id: number,
  name: string,
}

export type StockItem = {
  id: number,
  sku: string,
  defaultUnitCost: number,
}

export type StockItemSummary = StockCounts & {
  stockLocation: StockLocation,
  stockItem: StockItem,
  type: string,
}

export type StockItemFlat = StockCounts & StockItem & {
  type: string,
};

export type WarehouseInventorySummary = StockCounts & {
  stockLocation: StockLocation,
  stockItems: Array<StockItemFlat>,
}

export type WarehouseInventoryMap = {
  [stockLocationId: number]: WarehouseInventorySummary
}

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
      payload = {qty: qty, type, status: 'onHand'};
      action = 'increment';
    } else {
      payload = {qty: -qty, type, status: 'onHand'};
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
      const promises = _.map(stockItemChanges, (payload: Object, key: string) => {
        return dispatch(changeItemUnits(payload.id, payload.diff, payload.type));
      });

      dispatch(clearSkuItemsChanges(sku));

      return Promise.all(promises);
    }
  };
}

const initialState = {};

const reducer = createReducer({
  [_fetchSummary.succeeded]: (state, [payload, sku]) => {
    const stockItems: Array<StockItemSummary> = payload.summary;

    const inventoryDetailsByLocations = _.reduce(stockItems, (acc, itemSummary: StockItemSummary) => {
      const warehouseDetails =
        acc[itemSummary.stockLocation.id] = acc[itemSummary.stockLocation.id] || {
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
      ['stockItemChanges', sku, `${stockItem.type}-${stockItem.id}`], {diff, type: stockItem.type, id: stockItem.id}
    );
  },
  [clearSkuItemsChanges]: (state, sku) => {
    return assoc(state,
      ['stockItemChanges', sku], {}
    );
  }
}, initialState);

export default reducer;
