
import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { assoc } from 'sprout-data';
import createAsyncActions from '../async-utils';

const _fetchSummary = createAsyncActions(
  'inventory.sku-summary',
  (skuCode) => Api.get(`/inventory/skus/${skuCode}/summary`),
  (...args) => [...args]
);
export const fetchSummary = _fetchSummary.perform;

const _fetchDetails = createAsyncActions(
  'inventory.sku-details',
  (skuCode, warehouseId) => {
    return Api.get(`/inventory/skus/${skuCode}/${warehouseId}`);
  },
  (...args) => [...args]
);
export const fetchDetails = _fetchDetails.perform;

function parseSummaries(summaries) {
  return _.map(summaries, (summary) => {
    return {
      id: _.get(summary, ['warehouse', 'id']),
      name: _.get(summary, ['warehouse', 'name']),
      ...summary.counts
    };
  });
}

function parseDetails(payload) {
  return _.map(payload, (entry) => {
    return {
      skuType: entry.skuType,
      ...entry.counts
    };
  });
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
  [_fetchSummary.succeeded]: (state, [payload, sku]) => {
    if (!_.isArray(payload)) payload = [payload];

    const data = parseSummaries(payload);
    const tableData = convertToTableData(data);
    return assoc(state,
      [sku, 'summary', 'results'], tableData
    );
  },
  [_fetchDetails.succeeded]: (state, [payload, sku, warehouseId]) => {
    const data = parseDetails(payload);
    const tableData = convertToTableData(data);
    return assoc(state,
      [sku, 'details', 'isFetching'], false,
      [sku, 'details', 'failed'], false,
      [sku, warehouseId, 'results'], tableData
    );
  },
}, initialState);

export default reducer;
