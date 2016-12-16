
import _ from 'lodash';
import {createAction, createReducer} from 'redux-act';
import { update, assoc } from 'sprout-data';
import { updateItems } from './state-helpers';
import Api from '../lib/api';

const startFetching = createAction('INSIGHTS_START_FETCHING');
const receivedValues = createAction('INSIGHTS_RECEIVED',(keys, values, from, to, sizeSec, stepSec) => [keys, values, from, to, sizeSec, stepSec]);
const fetchFailed = createAction('INSIGHTS_FETCH_FAILED');
export const resetInsights = createAction('INSIGHTS_RESET');

export function fetchInsights(keys, from, to, sizeSec, stepSec) {
  return dispatch => {
    dispatch(startFetching());

    const keyStr = _.reduceRight(keys, (result, v) => {
      if(result) return `${result},${v.key}`;
      return v.key;
    }, "");

    const url = `time/values?keys=${keyStr}&a=${from}&b=${to}&size=${sizeSec}&step=${stepSec}&xy&sum`;
    return Api.get(url).then(
      values => dispatch(receivedValues(keys, values, from, to, sizeSec, stepSec)),
      err => dispatch(fetchFailed(err))
    );
  };
}

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
  [resetInsights]: () => {
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
  [fetchFailed]: (state, err) => {
    console.error(err);

    return assoc(state,
      ['isFetching'], false,
      ['err'], err
    );
  }
}, initialState);

export default reducer;
