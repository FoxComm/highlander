'use strict';

import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';
import { merge, get } from 'sprout-data';

export const actionTypes = {
  FETCH: 'FETCH',
  FETCHED: 'FETCHED',
  FETCH_FAILED: 'FETCH_FAILED',
  SET_FETCH_DATA: 'SET_FETCH_DATA'
};

/**
 *  @example for custom action in finite module:
 *
 *  import {fetchMeta, actionTypes} from './pagination';
 *  const fetchSmthSuccessThatRequiresCustomLogic = createAction('DESCRIPTION', null, fetchMeta(actionTypes.FETCH));
 *
 *  createReducer({
 *    [fetchSmthSuccessThatRequiresCustomLogic]: (state, ...) => {
 *    }
 *  });
 */
export function fetchMeta(paginationType) {
  return () => ({paginationType});
}

export function createActions(url, type) {
  const createFetchAction = actionType => {
    return createAction(`${type}_${actionType}`, null, fetchMeta(actionType));
  };

  const actionRequest = createFetchAction(actionTypes.FETCH);
  const actionSuccess = createFetchAction(actionTypes.FETCHED);
  const actionFail = createFetchAction(actionTypes.FETCH_FAILED);
  const actionSetFetchData = createFetchAction(actionTypes.SET_FETCH_DATA);

  const fetch = fetchData => dispatch => {
    dispatch(actionRequest());
    return Api.get(url, {from: fetchData.from, size: fetchData.size, sortBy: fetchData.sortBy})
      .then(orders => dispatch(actionSuccess(orders)))
      .catch(err => dispatch(actionFail(err, fetch)));
  };

  const setFetchData = (state, fetchData) => dispatch => {
    dispatch(actionSetFetchData(fetchData));
    dispatch(fetch({
      ...state,
      ...fetchData
    }));
  };

  return {
    fetch,
    setFetchData
  };
}

const initialState = {
  isFetching: false,
  rows: [],
  total: 0,
  from: 0,
  size: 25
};

export function reducer(reducer = state => state) {
  return (state, action) => {
    if (state === void 0) {
      state = merge(
        reducer(state, action) || {},
        initialState
      );
    }

    const paginationType = get(action, ['meta', 'paginationType']);

    if (paginationType) {
      const payload = action.payload;
      let data = [];

      if (payload !== undefined) {
        if (typeof payload === 'array') {
          data = payload;
        } else {
          data = payload.result;
        }
      }

      switch (paginationType) {
        case actionTypes.FETCH:
          return {
            ...state,
            isFetching: true
          };
        case actionTypes.FETCHED:
          return {
            ...state,
            isFetching: false,
            rows: data,
            total: payload.length
          };
        case actionTypes.FETCH_FAILED:
          console.error(payload);

          return {
            ...state,
            isFetching: false
          };
        case actionTypes.SET_FETCH_DATA:
          return {
            ...state,
            ...payload
          };
      }
    }

    return state;
  };
}

// default behaviour for simple cases
export default function(url, type, moduleReducer) {
  return {
    reducer: reducer(moduleReducer),
    actions: createActions(url, type)
  };
}
