import _ from 'lodash';
import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';

const scTypeRequested = createAction('SC_TYPE_REQUESTED');
const scTypeReceived = createAction('SC_TYPE_RECEIVED');
const scTypeFailed = createAction('SC_TYPE_FAILED');

export function fetchScTypes() {
  return dispatch => {
    dispatch(scTypeRequested());

    return Api.get('/store-credits/types')
      .then(json => dispatch(scTypeReceived(json)))
      .catch(err => dispatch(scTypeFailed(err)));
  };
}

const initialState = {};

const reducer = createReducer({
  [scTypeRequested]: state => {
    return {
      ...state,
      isFetching: true
    };
  },
  [scTypeReceived]: (state, json) => {
    const data = _.get(json, 'result', json);
    return {
      ...state,
      isFetching: false,
      types: data
    };
  },
  [scTypeFailed]: (state, err) => {
    console.error(err);
    return {
      ...state,
      isFetching: false
    };
  }
}, initialState);

export default reducer;
