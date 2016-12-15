
import _ from 'lodash';
import {createAction, createReducer} from 'redux-act';
import { update, assoc } from 'sprout-data';
import { updateItems } from './state-helpers';
import Api from '../lib/api';

const startFetching = createAction('INSIGHTS_START_FETCHING');
const receivedValues = createAction('INSIGHTS_RECEIVED',(insightKey, values, from, to, sizeSec, stepSec) => [insightKey, values, from, to, sizeSec, stepSec]);
const fetchFailed = createAction('INSIGHTS_FETCH_FAILED');
const setInsightKey = createAction('INSIGHTS_SET_INSIGHT_KEY', (insightKey) => [insightKey]);
export const resetInsights = createAction('INSIGHTS_RESET');

export function updateInsightKey(insightKey) { 
  return dispatch => { dispatch(setInsightKey(insightKey));};
}

export function fetchInsights(insightKey, from, to, sizeSec, stepSec) {
  return dispatch => {
    dispatch(startFetching());
    const url = `time/values?key=${insightKey}&a=${from}&b=${to}&size=${sizeSec}&step=${stepSec}&xy&sum`;
    return Api.get(url).then(
      values => dispatch(receivedValues(insightKey, values, from, to, sizeSec, stepSec)),
      err => dispatch(fetchFailed(err))
    );
  };
}

const initialState = {
  isFetching: null,
  err: null,
  values: [],
  sizeSec: 1,
  stepSec: 1,
  from: 0,
  to: 0,
  insightKey: "poo",
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
  [receivedValues]: (state, [insightKey, values, from, to, sizeSec, stepSec]) => {
        console.log("RESP");
        console.log(values);
        console.log(insightKey);
    const updater = _.flow(
      _.partialRight(assoc,
        ['insightKey'], insightKey,
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
  },
  [setInsightKey]: (state, [insightKey]) => {
    return assoc(state, ['insightKey'], insightKey);
  },
}, initialState);

export default reducer;
