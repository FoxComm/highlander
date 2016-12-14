
import _ from 'lodash';
import {createAction, createReducer} from 'redux-act';
import { update, assoc } from 'sprout-data';
import { updateItems } from './state-helpers';
import Api from '../lib/api';

const startFetching = createAction('INSIGHTS_START_FETCHING');
const receivedValues = createAction('INSIGHTS_RECEIVED');
const fetchFailed = createAction('INSIGHTS_FETCH_FAILED');
export const resetInsights = createAction('INSIGHTS_RESET');

function getValues(key, a, b, size, step) {
      const url = `henhouse/values?key=${key}&a${a}&b${b}&size=${size}&step=&{step}&xy`;
      return Api.get(url);
}

export function fetchInsights(key, a, b, size, step) {
  return dispatch => {
    dispatch(startFetching());
    getValues(key, a, b, size, step).then(
      response => {
        dispatch(receivedValues( {
            key: key,
            values: response.result,
            from_tm: a,
            to_tm: b,
            size_sec: size,
            step_sec: step,
          }
        ));
      },
      err => dispatch(fetchFailed(err))
    );
  };
}

const initialState = {
  isFetching: null,
  err: null,
  values: [],
  size_sec: 1,
  step_Sec: 1,
  from: 0,
  to: 0,
  key: "",
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
  [receivedValues]: (state, data) => {
    const updater = _.flow(
      _.partialRight(assoc,
        ['key'], data.key,
        ['values'], data.values,
        ['from'], data.from,
        ['to'], data.to,
        ['size_sec'], data.size_sec,
        ['step_sec'], data.step_sec,
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
}, initialState);

export default reducer;
