'use strict';

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';

export const requestRmas = createAction('RMAS_REQUEST');
export const receiveRmas = createAction('RMAS_RECEIVE');
export const updateRmas = createAction('RMAS_UPDATE');
export const failRmas = createAction('RMAS_FAIL', (err, source) => [err, source]);

export function fetchRmas() {
  return dispatch => {
    dispatch(requestRmas());
    return Api.get('/rmas')
      .then(rmas => dispatch(receiveRmas(rmas)))
      .catch(err => dispatch(failRmas(err, fetchRmas)));
  };
}

function updateItems(items, newItems) {
  return _.values({
    ..._.indexBy(items, 'id'),
    ..._.indexBy(newItems, 'id')
  });
}

const initialState = {
  isFetching: false,
  items: []
};

const reducer = createReducer({
  [requestRmas]: (state) => {
    return {
      ...state,
      isFetching: true
    };
  },
  [receiveRmas]: (state, payload) => {
    return {
      ...state,
      isFetching: false,
      items: payload.result
    };
  },
  [updateRmas]: (state, payload) => {
    return {
      ...state,
      items: updateItems(state.items, payload)
    };
  },
  [failRmas]: (state, [err, source]) => {
    console.error(err);

    if (source === fetchRmas) {
      return {
        ...state,
        isFetching: false
      };
    }

    return state;
  }
}, initialState);

export default reducer;
