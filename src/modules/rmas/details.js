'use strict';

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';

export const rmaSuccess = createAction('RMA_SUCCESS');
export const failRma = createAction('RMA_FAIL', (id, err, source) => [id, err, source]);
export const requestRma = createAction('RMA_REQUEST');

export function fetchRma(id) {
  return dispatch => {
    dispatch(requestRma(id));
    Api.get(`/rmas/${id}`)
      .then(rma => dispatch(rmaSuccess(rma)))
      .catch(err => dispatch(failRma(id, err, fetchRma)));
  };
}

const initialState = {
  isFetching: false,
  currentRma: {}
};

const reducer = createReducer({
  [requestRma]: (state, id) => {
    return {
      ...state,
      isFetching: true
    };
  },
  [rmaSuccess]: (state, payload) => {
    return {
      ...state,
      isFetching: false,
      currentRma: haveType(payload, 'rma')
    };
  },
  [failRma]: (state, [id, err, source]) => {
    console.error(err);

    return {
      ...state,
      isFetching: false
    };
  }
}, initialState);

export default reducer;
