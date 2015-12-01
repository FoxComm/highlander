import _ from 'lodash';
import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';

const reasonsRequested = createAction('REASONS_REQUESTED');
const reasonsReceived = createAction('REASONS_RECEIVED');
const reasonsFailed = createAction('REASONS_FAILED');

export function fetchReasons() {
  return dispatch => {
    dispatch(reasonsRequested());

    return Api.get('/reasons')
      .then(json => dispatch(reasonsReceived(json)))
      .catch(err => dispatch(reasonsFailed(err)));
  };
}

const initialState = {};

const reducer = createReducer({
  [reasonsRequested]: state => {
    return {
      ...state,
      isFetching: true
    };
  },
  [reasonsReceived]: (state, json) => {
    const data = _.get(json, 'result', json);
    return {
      ...state,
      isFetching: false,
      reasons: data
    };
  },
  [reasonsFailed]: (state, err) => {
    console.error(err);
    return state;
  }
}, initialState);

export default reducer;
