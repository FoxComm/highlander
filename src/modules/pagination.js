'use strict';

import _ from 'lodash';
import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';
import { merge, get, update } from 'sprout-data';

export const actionTypes = {
  FETCH: 'FETCH',
  FETCHED: 'FETCHED',
  FETCH_FAILED: 'FETCH_FAILED',
  SET_FETCH_PARAMS: 'SET_FETCH_PARAMS',
  ADD_ENTITY: 'ADD_ENTITY'
};

export function fetchMeta(namespace, actionType) {
  return (meta = {}) => ({
    ...meta,
    fetch: {
      actionType,
      namespace
    }
  });
}

export function pickFetchParams(state) {
  return {from: state.from, size: state.size, sortBy: state.sortBy};
}

export function makeCreateFetchAction(namespace, payloadReducer = null, metaReducer = _.identity) {
  return actionType => {
    return createAction(
      `${namespace}_${actionType}`,
      payloadReducer,
      _.flow(metaReducer, fetchMeta(namespace, actionType))
    );
  };
}


export function createActions(url, namespace) {
  const createFetchAction = makeCreateFetchAction(namespace);

  const actionRequest = createFetchAction(actionTypes.FETCH);
  const actionSuccess = createFetchAction(actionTypes.FETCHED);
  const actionFail = createFetchAction(actionTypes.FETCH_FAILED);
  const actionSetFetchParams = createFetchAction(actionTypes.SET_FETCH_PARAMS);

  const fetch = fetchData => dispatch => {
    dispatch(actionRequest());
    return Api.get(url, pickFetchParams(fetchData))
      .then(orders => dispatch(actionSuccess(orders)))
      .catch(err => dispatch(actionFail(err, fetch)));
  };

  const setFetchParams = (state, fetchParams) => dispatch => {
    dispatch(actionSetFetchParams(fetchParams));
    dispatch(fetch({
      ...state,
      ...fetchParams
    }));
  };

  return {
    fetch,
    setFetchParams
  };
}

const initialState = {
  isFetching: false,
  rows: [],
  total: 0,
  from: 0,
  size: 25
};

export function paginate(state = initialState, action) {
  const payload = action.payload;

  switch (action.type) {
    case actionTypes.FETCH:
      return {
        ...state,
        isFetching: true
      };
    case actionTypes.FETCHED:
      return {
        ...state,
        isFetching: false,
        rows: payload,
        total: payload.length
      };
    case actionTypes.ADD_ENTITY:
      return {
        ...state,
        total: state.total + 1
      };
    case actionTypes.FETCH_FAILED:
      console.error(payload);

      return {
        ...state,
        isFetching: false
      };
    case actionTypes.SET_FETCH_PARAMS:
      return {
        ...state,
        ...payload
      };
  }

  return state;
}

function defaultPaginateBehaviour(state, action, fetchActionType) {
  return paginate(state, {
    ...action,
    type: fetchActionType
  });
}

export function paginateReducer(namespace, reducer = state => state, updateBehaviour = defaultPaginateBehaviour) {
  return (state, action) => {
    if (state === void 0) {
      state = merge(
        reducer(state, action) || {},
        paginate(state, action)
      );
    }

    const actionType = get(action, ['meta', 'fetch', 'actionType']);
    const metaNamespace = get(action, ['meta', 'fetch', 'namespace']);

    if (actionType && metaNamespace === namespace) {
      state = updateBehaviour(state, action, actionType);
    }

    return reducer(state, action);
  };
}

// default behaviour for simple cases
export default function(url, namespace, moduleReducer) {
  return {
    reducer: paginateReducer(namespace, moduleReducer),
    actions: createActions(url, namespace)
  };
}
