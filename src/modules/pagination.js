'use strict';

import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';

export default (url, customReducers, customInitialState) => {
  const actionRequest = createAction(`REQUEST ${url}`);
  const actionSuccess = createAction(`SUCCESS ${url}`);
  const actionFail = createAction(`FAILED ${url}`, (err, source) => ({err, source}));
  const actionSetState = createAction(`SET_STATE ${url}`);

  const setState = (state, newState) => dispatch => {
    dispatch(actionSetState(newState));
    dispatch(fetch({
      ...state,
      ...newState
    }));
  };

  const fetch = state => dispatch => {
    dispatch(actionRequest());
    return Api.get(url, {from: state.from, size: state.size, sortBy: state.sortBy})
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
    [actionSetState]: (state, newState) => {
      return {
        ...state,
        ...newState
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
    setState,
    reducer
  };
};
