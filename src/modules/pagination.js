'use strict';

import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';

export default (url, customReducers, customInitialState) => {
  const actionRequest = createAction(`request ${url}`);
  const actionSuccess = createAction(`success ${url}`);
  const actionFail = createAction(`failed ${url}`, (err, source) => ({err, source}));
  const actionSetFrom = createAction(`set from ${url}`);
  const actionSetSize = createAction(`set size ${url}`);

  const setFrom = from => dispatch => dispatch(actionSetFrom(from));
  const setSize = size => dispatch => dispatch(actionSetSize(size));

  const fetch = state => dispatch => {
    dispatch(actionRequest());
    return Api.get(url, {from: state.from, size: state.size})
      .then(orders => dispatch(actionSuccess(orders)))
      .catch(err => dispatch(actionFail(err, fetch)));
  };

  const reducer = createReducer({
    ...customReducers,
    [actionRequest]: (state) => {
      return {
        ...state,
        isFetching: true
      };
    },
    [actionSuccess]: (state, payload) => {
      return {
        ...state,
        isFetching: false,
        rows: payload,
        total: payload.length
      };
    },
    [actionFail]: (state, {err, source}) => {
      console.error(err);
      if (source === fetch) {
        return {
          ...state,
          isFetching: false
        };
      }
      return state;
    },
    [actionSetFrom]: (state, from) => {
      return {
        ...state,
        from: Math.max(0, Math.min(state.total - 1, from))
      };
    },
    [actionSetSize]: (state, size) => {
      return {
        ...state,
        size
      };
    }
  }, {
    ...customInitialState,
    isFetching: false,
    rows: [],
    total: 0,
    from: 0,
    size: 25
  });

  return {
    fetch,
    setFrom,
    setSize,
    reducer
  };
};
