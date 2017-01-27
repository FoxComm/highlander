// libs
import _ from 'lodash';
import {createAction, createReducer} from 'redux-act';
import { assoc } from 'sprout-data';
import Api from '../lib/api';

// helpers
const fetchStatsForStatKey = (statKey, from, to, productId, size, statNames, channel) => {
  return dispatch => {
    dispatch(startFetching());

    const keys = `track.${channel}.product.${productId}.${statKey}`;
    const stepSize = `step=${size}&size=${size}`;
    const statsQuery = _.join(statNames, '&');

    const url = `time/values?keys=${keys}&a=${from}&b=${to}&${stepSize}&${statsQuery}`;

    return Api.get(url).then(
      chartValues => dispatch(productStatsForStatKeyReceivedValues(chartValues, keys, size, from, to)),
      err => dispatch(fetchFailed(err))
    );
  };
};

// action types
/* generic */
const startFetching = createAction('ANALYTICS_START_FETCHING');
const fetchFailed = createAction('ANALYTICS_FETCH_FAILED');
const startFetchingStats = createAction('ANALYTICS_START_FETCHING_STATS');
const fetchStatsFailed = createAction('ANALYTICS_FETCH_STATS_FAILED');
export const resetAnalytics = createAction('ANALYTICS_RESET');
/* time */
const receivedValues = createAction('ANALYTICS_RECEIVED',
  (keys, chartValues, from, to, sizeSec, stepSec) => [keys, chartValues, from, to, sizeSec, stepSec]
);
/* stats */
const productConversionReceivedValues = createAction('ANALYTICS_PRODUCT_CONVERSION_RECEIVED',
  (chartValues) => [chartValues]
);
const productStatsForStatKeyReceivedValues = createAction('ANALYTICS_PRODUCT_STATS_FOR_STAT_KEY_RECEIVED',
  (chartValues, keys, size, from, to) => [chartValues, keys, size, from, to]
);
const productStatsReceivedValues = createAction('ANALYTICS_PRODUCT_STATS_RECEIVED', (stats) => [stats]);

// actions
/* time */
export function fetchAnalytics(keys, from, to, sizeSec, stepSec) {
  return dispatch => {
    dispatch(startFetching());

    const keyStr = _.reduceRight(keys, (result, v) => {
      if(result) return `${result},${v.key}`;
      return v.key;
    }, '');

    const url = `time/values?keys=${keyStr}&a=${from}&b=${to}&size=${sizeSec}&step=${stepSec}&xy&sum`;
    return Api.get(url).then(
      chartValues => dispatch(receivedValues(keys, values, from, to, sizeSec, stepSec)),
      err => dispatch(fetchFailed(err))
    );
  };
}
/* stats */
export function fetchProductConversion(productId) {
  return dispatch => {
    dispatch(startFetching());

    const url = `stats/productFunnel/${productId}`;

    return Api.get(url).then(
      chartValues => dispatch(productConversionReceivedValues(chartValues)),
      err => dispatch(fetchFailed(err))
    );
  };
}
export function fetchProductStats(productId, channel = 1) {
  return dispatch => {
    dispatch(startFetching());

    const url = `stats/productStats/${channel}/${productId}`;

    return Api.get(url).then(
      stats => dispatch(productStatsReceivedValues(stats)),
      err => dispatch(fetchStatsFailed(err))
    );
  };
}
export function fetchProductTotalRevenue(from, to, productId, size, channel = 1) {
  return fetchStatsForStatKey('revenue', from, to, productId, size, ['agg', 'xy'], channel);
}
export function fetchProductTotalOrders(from, to, productId, size, channel = 1) {
  return fetchStatsForStatKey('purchase-quantity', from, to, productId, size, ['sum', 'xy'], channel);
}

// redux store
const initialState = {
  isFetching: null,
  isFetchingStats: null,
  err: null,
  chartValues: {},
  sizeSec: -1,
  stepSec: -1,
  from: -1,
  to: -1,
  keys: '',
  verbs: [],
  stats: {},
};

const reducer = createReducer({
  [startFetching]: state => {
    return assoc(state,
      ['isFetching'], true,
      ['err'], null
    );
  },
  [startFetchingStats]: state => {
    return assoc(state,
      ['isFetchingStats'], true,
      ['err'], null
    );
  },
  [resetAnalytics]: () => {
    return initialState;
  },
  [receivedValues]: (state, [keys, chartValues, from, to, sizeSec, stepSec]) => {
    const updater = _.flow(
      _.partialRight(assoc,
        ['keys'], keys,
        ['chartValues'], chartValues,
        ['from'], from,
        ['to'], to,
        ['sizeSec'], sizeSec,
        ['stepSec'], stepSec,
        ['isFetching'], false
      )
    );

    return updater(state);
  },
  [productConversionReceivedValues]: (state, [chartValues]) => {
    const updater = _.flow(
      _.partialRight(assoc,
        ['chartValues'], chartValues,
        ['isFetching'], false,
      )
    );

    return updater(state);
  },
  [productStatsForStatKeyReceivedValues]: (state, [chartValues, keys, size, from, to]) => {
    const updater = _.flow(
      _.partialRight(assoc,
        ['isFetching'], false,
        ['chartValues'], chartValues,
        ['sizeSec'], size,
        ['stepSec'], size,
        ['from'], from,
        ['to'], to,
        ['keys'], keys,
      )
    );

    return updater(state);
  },
  [productStatsReceivedValues]: (state, [stats]) => {
    const updater = _.flow(
      _.partialRight(assoc,
        ['stats'], stats,
        ['isFetchingStats'], false,
      )
    );

    return updater(state);
  },
  [fetchFailed]: (state, err) => {
    console.error(err);

    return assoc(state,
      ['isFetching'], false,
      ['err'], err
    );
  },
  [fetchStatsFailed]: (state, err) => {
    console.error(err);

    return assoc(state,
      ['isFetchingStats'], false,
      ['err'], err
    );
  }
}, initialState);

export default reducer;
