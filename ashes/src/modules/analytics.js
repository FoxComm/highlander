// libs
import _ from 'lodash';
import {createAction, createReducer} from 'redux-act';
import { assoc } from 'sprout-data';
import Api from '../lib/api';

// action types
/* generic */
const startFetching = createAction('ANALYTICS_START_FETCHING');
const startFetchingStats = createAction('ANALYTICS_START_FETCHING_STATS');
const fetchFailed = createAction('ANALYTICS_FETCH_FAILED');
const fetchStatsFailed = createAction('ANALYTICS_FETCH_STATS_FAILED');
export const resetAnalytics = createAction('ANALYTICS_RESET');
/* time */
const receivedValues = createAction('ANALYTICS_RECEIVED',
  (keys, chartValues, from, to, sizeSec, stepSec) => [keys, chartValues, from, to, sizeSec, stepSec]
);
/* stats */
const productConversionReceivedValues = createAction('ANALYTICS_PRODUCTCONVERSION_RECEIVED',
  (chartValues) => [chartValues]
);
const productTotalRevenueReceivedValues = createAction('ANALYTICS_PRODUCTTOTALREVENUE_RECEIVED',
  (chartValues) => [chartValues]
);
const productStatsReceivedValues = createAction('ANALYTICS_PRODUCTSTATS_RECEIVED', (stats) => [stats]);

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
export function fetchProductTotalRevenue() {
  return dispatch => {
    dispatch(startFetching());
    
    const url = `stats/productSum/list/9`;

    return Api.get(url).then(
      chartValues => dispatch(productTotalRevenueReceivedValues(chartValues)),
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

// redux store
const initialState = {
  isFetching: null,
  isFetchingStats: null,
  err: null,
  chartValues: {},
  sizeSec: 1,
  stepSec: 1,
  from: 0,
  to: 0,
  keys: [],
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
  [productTotalRevenueReceivedValues]: (state, [chartValues]) => {
    const updater = _.flow(
      _.partialRight(assoc,
        ['chartValues'], chartValues,
        ['isFetching'], false,
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
