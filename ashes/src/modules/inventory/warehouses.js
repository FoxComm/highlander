
import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { assoc } from 'sprout-data';

const warehousesFetchSummaryStart = createAction('WAREHOUSES_FETCH_SUMMARY_START');
const warehousesFetchSummarySuccess = createAction('WAREHOUSES_FETCH_SUMMARY_SUCCESS',
                                                   (sku, payload) => [sku, payload]);
const warehousesFetchSummaryFailed = createAction('WAREHOUSES_FETCH_SUMMARY_ERROR', (sku, err) => [sku, err]);
const warehousesFetchDetailsStart = createAction('WAREHOUSES_FETCH_DETAILS_START');
const warehousesFetchDetailsSuccess = createAction('WAREHOUSES_FETCH_DETAILS_SUCCESS',
                                                   (sku, warehouseId, payload) => [sku, warehouseId, payload]);
const warehousesFetchDetailsFailed = createAction('WAREHOUSES_FETCH_DETAILS_ERROR', (sku, err) => [sku, err]);

export function fetchSummary(skuCode) {
  return dispatch => {
    dispatch(warehousesFetchSummaryStart(skuCode));
    Api.get(`/inventory/skus/${skuCode}/summary`).then(
      data => dispatch(warehousesFetchSummarySuccess(skuCode, data)),
      err => dispatch(warehousesFetchSummaryFailed(err))
    );
  };
}

export function fetchDetails(skuCode, warehouseId) {
  return dispatch => {
    dispatch(warehousesFetchDetailsStart(skuCode));
    Api.get(`/inventory/skus/${skuCode}/${warehouseId}`).then(
      data => dispatch(warehousesFetchDetailsSuccess(skuCode, warehouseId, data)),
      err => dispatch(warehousesFetchDetailsFailed(err))
    );
  };
}

function parseSummaries(summaries) {
  const data = _.map(summaries, (summary) => {
    const result = {
      id: _.get(summary, ['warehouse', 'id']),
      name: _.get(summary, ['warehouse', 'name']),
      ...summary.counts
    };
    return result;
  });
  return data;
}

function parseDetails(payload) {
  const data = _.map(payload, (entry) => {
    const result = {
      skuType: entry.skuType,
      ...entry.counts
    };
    return result;
  });
  return data;
}

function convertToTableData(data) {
  return {
    rows: data,
    total: data.length,
    from: 0,
    size: 25,
  };
}

const initialState = {};

const reducer = createReducer({
  [warehousesFetchSummaryStart]: (state, sku) => {
    return assoc(state, [sku, 'summary', 'isFetching'], true);
  },
  [warehousesFetchSummarySuccess]: (state, [sku, payload]) => {
    const data = parseSummaries(payload);
    const tableData = convertToTableData(data);
    return assoc(state,
      [sku, 'summary', 'isFetching'], false,
      [sku, 'summary', 'failed'], false,
      [sku, 'summary', 'results'], tableData
    );
  },
  [warehousesFetchDetailsStart]: (state, sku) => {
    return assoc(state, [sku, 'details', 'isFetching'], true);
  },
  [warehousesFetchDetailsSuccess]: (state, [sku, warehouseId, payload]) => {
    const data = parseDetails(payload);
    const tableData = convertToTableData(data);
    return assoc(state,
      [sku, 'details', 'isFetching'], false,
      [sku, 'details', 'failed'], false,
      [sku, warehouseId, 'results'], tableData
    );
  },
  [warehousesFetchSummaryFailed]: (state, [sku, err]) => {
    console.error(err);
    return assoc(state,
      [sku, 'summary', 'isFetching'], false,
      [sku, 'summary', 'failed'], true
    );
  },
  [warehousesFetchDetailsFailed]: (state, [sku, err]) => {
    console.error(err);
    return assoc(state,
      [sku, 'details', 'isFetching'], false,
      [sku, 'details', 'failed'], true
    );
  },
}, initialState);

export default reducer;
