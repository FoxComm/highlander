// libs
import _ from 'lodash';
import {createAction, createReducer} from 'redux-act';
import { assoc } from 'sprout-data';
import Api from '../lib/api';
import SHA1 from 'crypto-js/sha1';

// helpers
const fetchStatsForStatKey = (statType, statKey, from, to, productId, size, statNames, channel) => {
  return dispatch => {
    dispatch(startFetching());

    const objHash = SHA1(`products/${productId}`).toString();
    const keys = `track.${channel}.${statType}.${objHash}.${statKey}`;
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
const resetAnalytics = createAction('ANALYTICS_RESET');
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
/* generic */
export function resetAnalyticsValues() {
  return dispatch => {
    dispatch(resetAnalytics());
  };
}
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

    const baseUrl = 'stats/productFunnel/';
    const objHash = SHA1(`products/${productId}`).toString();
    const productUrl = `${baseUrl}${objHash}`;

    const fetchAvgProductConversionValues = () => {
      return Api.get(baseUrl).then(
        avgChartValues => { return avgChartValues; },
        err => dispatch(fetchFailed(err))
      );
    };

    const fetchProductConversionValues = (avgConversionValues) => {
      return Api.get(productUrl).then(
        chartValues => {
          const chartValuesWithAvgs = Object.assign({}, chartValues, { Average: avgConversionValues });
          dispatch(productConversionReceivedValues(chartValuesWithAvgs));
        },
        err => dispatch(fetchFailed(err))
      );
    };

    return fetchAvgProductConversionValues()
      .then(fetchProductConversionValues);
  };
}
export function fetchProductStats(productId, channel = 1) {
  return dispatch => {
    dispatch(startFetchingStats());

    const objHash = SHA1(`products/${productId}`).toString();
    const url = `stats/productStats/${channel}/${objHash}`;

    return Api.get(url).then(
      stats => dispatch(productStatsReceivedValues(stats)),
      err => dispatch(fetchStatsFailed(err))
    );
  };
}
export function fetchProductTotalRevenue(from, to, productId, size, channel = 1, statType = 'product') {
  return fetchStatsForStatKey(statType, 'revenue', from, to, productId, size, ['agg', 'xy'], channel);
}
export function fetchProductTotalOrders(from, to, productId, size, channel = 1, statType = 'product') {
  return fetchStatsForStatKey(statType, 'purchase-quantity', from, to, productId, size, ['agg', 'xy'], channel);
}
export function fetchProductTotalInCarts(from, to, productId, size, channel = 1, statType = 'product') {
  return fetchStatsForStatKey(statType, 'cart', from, to, productId, size, ['agg', 'xy'], channel);
}
export function fetchProductTotalPdPViews(from, to, productId, size, channel = 1, statType = 'product') {
  return fetchStatsForStatKey(statType, 'pdp', from, to, productId, size, ['sum', 'xy'], channel);
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
