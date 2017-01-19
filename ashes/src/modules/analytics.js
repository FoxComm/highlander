// libs
import _ from 'lodash';
import {createAction, createReducer} from 'redux-act';
import { assoc } from 'sprout-data';
import Api from '../lib/api';

// action types
/* generic */
const startFetching = createAction('ANALYTICS_START_FETCHING');
const fetchFailed = createAction('ANALYTICS_FETCH_FAILED');
export const resetAnalytics = createAction('ANALYTICS_RESET');
/* time */
const receivedValues = createAction('ANALYTICS_RECEIVED',
  (keys, values, from, to, sizeSec, stepSec) => [keys, values, from, to, sizeSec, stepSec]
);
/* stats */
const productConversionReceivedValues = createAction('ANALYTICS_PRODUCTCONVERSION_RECEIVED',
  (values) => [values]
);

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
      values => dispatch(receivedValues(keys, values, from, to, sizeSec, stepSec)),
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
      values => dispatch(productConversionReceivedValues(values)),
      err => dispatch(fetchFailed(err))
    );
  };
}

// redux store
const initialState = {
  isFetching: null,
  err: null,
  values: {},
  sizeSec: 1,
  stepSec: 1,
  from: 0,
  to: 0,
  keys: [],
  verbs: []
};
const reducer = createReducer({
  [startFetching]: state => {
    return assoc(state,
      ['isFetching'], true,
      ['err'], null
    );
  },
  [resetAnalytics]: () => {
    return initialState;
  },
  [receivedValues]: (state, [keys, values, from, to, sizeSec, stepSec]) => {
    const updater = _.flow(
      _.partialRight(assoc,
        ['keys'], keys,
        ['values'], values,
        ['from'], from,
        ['to'], to,
        ['sizeSec'], sizeSec,
        ['stepSec'], stepSec,
        ['isFetching'], false
      )
    );

    return updater(state);
  },
  [productConversionReceivedValues]: (state, [values]) => {
    const updater = _.flow(
      _.partialRight(assoc,
        ['values'], values,
        ['isFetching'], false,
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
  }
}, initialState);

export default reducer;
