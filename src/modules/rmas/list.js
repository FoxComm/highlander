'use strict';

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';

export const requestRmas = createAction('RMAS_REQUEST');
export const receiveRmas = createAction('RMAS_RECEIVE');
export const updateRmas = createAction('RMAS_UPDATE');
export const failRmas = createAction('RMAS_FAIL', (err, source) => ({err, source}));

export function fetchRmas() {
  return dispatch => {
    dispatch(requestRmas());
    return Api.get('/returns')
      .then(rmas => dispatch(receiveRmas(rmas)))
      .catch(err => dispatch(failRmas(err, fetchRmas)));
  };
}

function shouldFetchRmas(state) {
  const rmas = state.rmas.list;
  if (!rmas) {
    return true;
  } else if (rmas.isFetching) {
    return false;
  }
  return rmas.didInvalidate;
}

export function fetchRmasIfNeeded() {
  return (dispatch, getState) => {
    if (shouldFetchRmas(getState())) {
      return dispatch(fetchRmas());
    }
  }
}

function updateItems(items, newItems) {
  return _.values({
    ..._.indexBy(items, 'id'),
    ..._.indexBy(newItems, 'id')
  });
}

const initialState = {
  isFetching: false,
  didInvalidate: true,
  items: []
};

const reducer = createReducer({
  [requestRmas]: (state) => {
    return {
      ...state,
      isFetching: true,
      didInvalidate: false
    };
  },
  [receiveRmas]: (state, payload) => {
    return {
      ...state,
      isFetching: false,
      didInvalidate: false,
      items: payload
    };
  },
  [updateRmas]: (state, payload) => {
    return {
      ...state,
      items: updateItems(state.items, payload)
    };
  },
  [failRmas]: (state, {err, source}) => {
    console.error(err);

    if (source === fetchRmas) {
      return {
        ...state,
        isFetching: false,
        didInvalidate: false
      };
    }

    return state;
  }
}, initialState);

export default reducer;
