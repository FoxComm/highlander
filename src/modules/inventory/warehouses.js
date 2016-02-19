
import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';

const warehousesFetchSummaryStart = createAction('WAREHOUSES_FETCH_SUMMARY_START');
const warehousesFetchSummarySuccess = createAction('WAREHOUSES_FETCH_SUMMARY_SUCCESS', (sku, payload) => [sku, payload]);
const warehousesFetchFailed = createAction('WAREHOUSES_FETCH_ERROR', (sku, err) => [sku, err]);
const warehousesFetchDetailsStart = createAction('WAREHOUSES_FETCH_DETAILS_START');
const warehousesFetchDetailsSuccess = createAction('WAREHOUSES_FETCH_DETAILS_SUCCESS', (sku, warehouseId, payload) => [sku, warehouseId, payload]);

export function fetchSummary(skuCode) {
  return dispatch => {
    dispatch(warehousesFetchSummaryStart(skuCode));
    Api.get(`/inventory/skus/${skuCode}/summary`).then(
      data => dispatch(warehousesFetchSummarySuccess(skuCode, data)),
      err => dispatch(warehousesFetchFailed(err))
    );
  };
}

export function fetchDetails(skuCode, warehouseId) {
  return dispatch => {
    dispatch(warehousesFetchDetailsStart(skuCode));
    Api.get(`/inventory/skus/${skuCode}/${warehouseId}`).then(
      data => dispatch(warehousesFetchDetailsSuccess(skuCode, warehouseId, data)),
      err => dispatch(warehousesFetchFailed(err))
    );
  };
}

const initialState = {};

const reducer = createReducer({
  [warehousesFetchSummaryStart]: (state, sku) => {
    return assoc(state, [sku, 'summary', 'isFetching'], true);
  },
  [warehousesFetchSummarySuccess]: (state, [sku, payload]) => {
    console.log(payload);
    return assoc(state,
      [sku, 'summary', 'isFetching'], false,
      [sku, 'summary', 'results'], payload
    );
  },
  [warehousesFetchDetailsStart]: (state, sku) => {
    return assoc(state, [sku, 'details', 'isFetching'], true);
  },
  [warehousesFetchDetailsStart]: (state, [sku, warehouseId, payload]) => {
    console.log(payload);
    return assoc(state,
      [sku, 'details', 'isFetching'], false,
      [sku, warehouseId, 'results'], payload
    );
  },
  [warehousesFetchFailed]: (state, [sku, err]) => {
    console.error(err);
    return assoc(state,
      [sku, 'summary', 'isFetching'], false,
      [sku, 'details', 'isFetching'], false
    );
  },
}, initialState);

export default reducer;
