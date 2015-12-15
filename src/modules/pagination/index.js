import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { merge, get, update } from 'sprout-data';

export const DEFAULT_PAGE_SIZE = 50;

export const actionTypes = {
  FETCH: 'FETCH',
  RECEIVED: 'RECEIVED',
  FETCH_FAILED: 'FETCH_FAILED',
  SET_FETCH_PARAMS: 'SET_FETCH_PARAMS',
  ADD_ENTITY: 'ADD_ENTITY',
  REMOVE_ENTITY: 'REMOVE_ENTITY',
  ADD_ENTITIES: 'ADD_ENTITIES',
  RESET: 'RESET',
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
    from: get(extraState, 'from', state && state.from),
    size: get(extraState, 'size', state && state.size),
    sortBy: get(extraState, 'sortBy', state && state.sortBy)
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
    result[name] = createFetchAction(type);
  });
}

/**
 * Creates async and simple actions for given namespace and url
 * @param {String|Function} url can be string or method like this: (fetchData) => string
 * @param {*} namespace
 * @param {Function} [payloadReducer]
 */
export function createActions(url, namespace, payloadReducer) {
  const fetchActions = createFetchActions(namespace, payloadReducer);
  const {
    actionFetch,
    actionReceived,
    actionFetchFailed,
    actionSetFetchParams
  } = fetchActions;

  const fetch = fetchData => dispatch => {
    dispatch(actionFetch());
    const finalUrl = _.isString(url) ? url : url(fetchData);

    return Api.get(finalUrl, pickFetchParams(fetchData))
      .then(
        result => dispatch(actionReceived(result)),
        err => dispatch(actionFetchFailed(err))
      );
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
    setFetchParams,
    ...fetchActions
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
        rows: get(payload, 'result', payload),
        total: get(payload, ['pagination', 'total'], payload.length)
      };
    case actionTypes.ADD_ENTITY:
      return {
        ...state,
        rows: [payload, ...state.rows],
        total: state.total + 1
      };
    case actionTypes.ADD_ENTITIES:
      return {
        ...state,
        rows: [...payload, ...state.rows],
        total: state.total + payload.length
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
    case actionTypes.RESET:
      return initialState;
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
        updateBehaviour(state, action)
      );
    }

    const actionType = get(action, ['meta', 'fetch', 'actionType']);
    const actionNamespace = get(action, ['meta', 'fetch', 'namespace']);

    if (actionType && actionNamespace === namespace) {
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
