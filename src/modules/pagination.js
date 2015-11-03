'use strict';

import _ from 'lodash';
import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';
import { merge, get, update } from 'sprout-data';

export const DEFAULT_PAGE_SIZE = 50;

export const actionTypes = {
  FETCH: 'FETCH',
  RECEIVED: 'RECEIVED',
  FETCH_FAILED: 'FETCH_FAILED',
  SET_FETCH_PARAMS: 'SET_FETCH_PARAMS',
  ADD_ENTITY: 'ADD_ENTITY',
  REMOVE_ENTITY: 'REMOVE_ENTITY'
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

export function pickFetchParams(state, extraState = {}) {
  return {
    from: _.get(extraState, 'from', state && state.from) || null, // we don't want pass 0 to phoenix
    size: _.get(extraState, 'size', state && state.size),
    sortBy: _.get(extraState, 'sortBy', state && state.sortBy)
  };
}

export function makeCreateFetchAction(namespace, payloadReducer = null, metaReducer = _.noop) {
  return actionType => {
    return createAction(
      `${namespace}_${actionType}`,
      payloadReducer,
      _.flow(metaReducer, fetchMeta(namespace, actionType))
    );
  };
}

export function createFetchActions(namespace, payloadReducer, metaReducer) {
  const createFetchAction = makeCreateFetchAction(namespace, payloadReducer, metaReducer);

  return _.transform(actionTypes, (result, type) => {
    const name = _.camelCase(`action_${type}`);
    result[name] =createFetchAction(type);
  });
}

export function createActions(url, namespace) {
  const {
    actionFetch,
    actionReceived,
    actionFetchFailed,
    actionSetFetchParams
  } = createFetchActions(namespace);

  const fetch = fetchData => dispatch => {
    dispatch(actionFetch());
    return Api.get(url, pickFetchParams(fetchData))
      .then(orders => dispatch(actionReceived(orders)))
      .catch(err => dispatch(actionFetchFailed(err, fetch)));
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
  size: DEFAULT_PAGE_SIZE
};

export function paginate(state = initialState, action) {
  const payload = action.payload;

  switch (action.type) {
    case actionTypes.FETCH:
      return {
        ...state,
        isFetching: true
      };
    case actionTypes.RECEIVED:
      return {
        ...state,
        isFetching: false,
        rows: _.get(payload, 'result', payload),
        total: _.get(payload, 'pagination.total', payload.length)
      };
    case actionTypes.ADD_ENTITY:
      return {
        ...state,
        rows: [payload, ...state.rows],
        total: state.total + 1
      };
    case actionTypes.REMOVE_ENTITY:
      return {
        ...state,
        rows: _.reject(state.rows, payload),
        total: state.total - 1
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
