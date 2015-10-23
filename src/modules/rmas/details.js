'use strict';

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';

export const receiveRma = createAction('RMA_RECEIVE', (id, rma) => [id, rma]);
export const failRma = createAction('RMA_FAIL', (id, err, source) => [id, err, source]);
export const requestRma = createAction('RMA_REQUEST');

function shouldFetchRma(id, state) {
  const entry = state.rmas.details[id];
  if (!entry) {
    return true;
  } else if (entry.isFetching) {
    return false;
  }
  return entry.didInvalidate;
}

export function fetchRma(id) {
  return dispatch => {
    dispatch(requestRma(id));
    Api.get(`/returns/${id}`)
      .then(rma => dispatch(receiveRma(id, rma)))
      .catch(err => dispatch(failRma(id, err, fetchRma)));
  };
}

export function fetchRmaIfNeeded(id) {
  return (dispatch, getState) => {
    if (shouldFetchRma(id, getState())) {
      dispatch(fetchRma(id));
    }
  };
}

const initialState = {};

const reducer = createReducer({
  [requestRma]: (entries, id) => {
    return {
      ...entries,
      [id]: {
        ...entries[id],
        isFetching: true,
        didInvalidate: false,
        err: null
      }
    };
  },
  [receiveRma]: (state, [id, rma]) => {
    return {
      ...state,
      [id]: {
        err: null,
        isFetching: false,
        didInvalidate: false,
        rma
      }
    };
  },
  [failRma]: (state, [id, err, source]) => {
    console.error(err);

    return {
      ...state,
      [id]: {
        ...state[id],
        err,
        ...(
          source === fetchRma ? {
            isFetching: false,
            didInvalidate: false
          } : {}
        )
      }
    };
  }
}, initialState);

export default reducer;
