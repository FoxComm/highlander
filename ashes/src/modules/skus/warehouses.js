// @flow weak

import _ from 'lodash';
import { assoc } from 'sprout-data';
import Api from 'lib/api';
import { createAction, createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';

export const updateSkuItemsCount = createAction(
  'SKU_UPDATE_ITEMS_COUNT',
  (skuId, stockItem, qty) => [skuId, stockItem, qty]
);

const clearSkuItemsChanges = createAction('SKU_CLEAR_ITEMS_CHANGES');

export type StockItemSummary = TStockCounts & {
  stockLocation: TStockLocation,
  stockItem: TStockItem,
  type: string,
  sku: string,
}

export type StockItemFlat = TStockCounts & TStockItem & {
  type: string,
};

export type WarehouseInventorySummary = TStockCounts & {
  stockLocation: TStockLocation,
  stockItems: Array<StockItemFlat>,
}

export type WarehouseInventoryMap = {
  [stockLocationId: number]: WarehouseInventorySummary
}

const _fetchSummary = createAsyncActions(
  'inventory-summary',
  (skuId: number, skuCode: string) => {
    return Api.get(`/inventory/summary/${skuCode}`).then(response => [response, skuId]);
  },
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

export function pushStockItemChanges(skuId) {
  return (dispatch, getState) => {
    const stockItemChanges = _.get(getState(), ['skus', 'warehouses', 'stockItemChanges', skuId]);

    if (stockItemChanges) {
      const promises = _.map(stockItemChanges, (payload: Object, key: string) => {
        return dispatch(changeItemUnits(payload.id, payload.diff, payload.type));
      });

      dispatch(clearSkuItemsChanges(skuId));

      return Promise.all(promises);
    }
  };
}

const initialState = {};

const reducer = createReducer({
  [_fetchSummary.succeeded]: (state, [payload, skuId]) => {
    const stockItems: Array<StockItemSummary> = payload.summary;

    const inventoryDetailsByLocations = _.reduce(stockItems, (acc, itemSummary: StockItemSummary) => {
      const warehouseDetails =
        acc[itemSummary.stockLocation.id] = acc[itemSummary.stockLocation.id] || {
        stockItems: [],
        stockLocation: itemSummary.stockLocation,
        sku: itemSummary.sku, // since we have only one sku in response we can do it
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
      ['details', skuId], inventoryDetailsByLocations
    );
  },
  [updateSkuItemsCount]: (state, [skuId, stockItem, diff]) => {
    return assoc(state,
      ['stockItemChanges', skuId, `${stockItem.type}-${stockItem.id}`], {diff, type: stockItem.type, id: stockItem.id}
    );
  },
  [clearSkuItemsChanges]: (state, skuId) => {
    return assoc(state,
      ['stockItemChanges', skuId], {}
    );
  }
}, initialState);

export default reducer;
