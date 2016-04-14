import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { updateItems as _updateItems } from '../state-helpers';
import { createNsAction } from '../utils';

export const DEFAULT_PAGE_SIZE = 50;

export const DEFAULT_PAGE_SIZES = [
  ['25', 'View 25'],
  ['50', 'View 50'],
  ['75', 'View 75'],
  ['100', 'View 100'],
];

const INITIAL_STATE = {
  // isFetching = null, - fetching wasn't started yet
  // isFetching = true, - fetching was started
  // isFetching = false, - fetching was finished
  isFetching: null,
  failed: false,
  rows: [],
  total: 0,
  from: 0,
  size: DEFAULT_PAGE_SIZE
};

/**
 * @param {Function} fetcher Data fetch function
 * @param {Object} actions Actions exposed on different search states
 * @param {Function} findSearchState Function to find current search for process fetching
 *
 * @returns {function(): function()} Async action creator
 */
export function makeFetchAction(fetcher, actions, findSearchState) {
  let fetchPromise;

  return (...args) => (dispatch, getState) => {
    const searchState = findSearchState(getState());

    if (!searchState.isFetching && !searchState.isRefreshing) {
      dispatch(actions.searchStart(...args));

      fetchPromise = fetcher.apply({ searchState, getState, dispatch }, args)
        .then(
          result => {
            if (_.isEmpty(result.error)) {
              return dispatch(actions.searchSuccess(result, {refreshed: false}));
            } else {
              return dispatch(actions.searchFailure(result));
            }
          },
          err => dispatch(actions.searchFailure(err))
        );
    }

    return fetchPromise;
  };
}

// like makeFetchAction but doesn't fire searchStart actions
// Used for refresh data but don't fire loading animation
// and resetting PilledInputs, etc, etc.
export function makeRefreshAction(fetcher, actions, findSearchState) {
  let fetchPromise;

  return (...args) => (dispatch, getState) => {
    const searchState = findSearchState(getState());

    if (!searchState.isFetching && !searchState.isRefreshing) {
      dispatch(actions.refreshStart(...args));

      fetchPromise = fetcher.apply({ searchState, getState, dispatch }, args)
        .then(
          result => {
            if (_.isEmpty(result.error)) {
              return dispatch(actions.searchSuccess(result, {refreshed: true}));
            } else {
              return dispatch(actions.searchFailure(result));
            }
          },
          err => dispatch(actions.searchFailure(err))
        );
    }

    return fetchPromise;
  };
}

function makePagination(namespace, fetcher = null, findSearchInState = null, initialState = {}) {

  initialState = {
    ...INITIAL_STATE,
    ...initialState
  };

  if (!findSearchInState) {
    findSearchInState = state => _.get(state, namespace);
  }

  const _createAction = (...args) => {
    return createNsAction(namespace, ...args);
  };

  const searchStart = _createAction('SEARCH_START', (...args) => args);
  const refreshStart = _createAction('REFRESH_START', (...args) => args);
  const searchSuccess = _createAction('SEARCH_SUCCESS', (...args) => args);
  const searchFailure = _createAction('SEARCH_FAILURE');
  const updateState = _createAction('UPDATE_STATE');
  const addEntity = _createAction('ADD_ENTITY');
  const addEntities = _createAction('ADD_ENTITIES');
  const removeEntity = _createAction('REMOVE_ENTITY');
  const resetSearch = _createAction('RESET_SEARCH');
  const updateItems = _createAction('UPDATE_ITEMS');

  const fetch = makeFetchAction(fetcher, { searchStart, searchSuccess, searchFailure }, findSearchInState);

  const updateStateAndFetch = (newState, ...args) => {
    return dispatch => {
      dispatch(updateState(newState));
      dispatch(fetch(...args));
    };
  };

  const reducer = createReducer({
    [searchStart]: state => {
      return {
        ...state,
        failed: false,
        isFetching: true
      };
    },
    [refreshStart]: state => {
      return {
        ...state,
        failed: false,
        isRefreshing: true,
      };
    },
    [searchSuccess]: (state, [response, opts]) => {
      return {
        ...state,
        ...opts,
        failed: false,
        isFetching: false,
        isRefreshing: false,
        rows: _.get(response, 'result', response),
        total: _.get(response, ['pagination', 'total'], response.length)
      };
    },
    [searchFailure]: (state, err) => {
      console.error(err);

      return {
        ...state,
        failed: true,
        isFetching: false,
        isRefreshing: false,
      };
    },
    [addEntity]: (state, entity) => {
      return {
        ...state,
        rows: [entity, ...state.rows],
        total: state.total + 1
      };
    },
    [addEntities]: (state, entities) => {
      return {
        ...state,
        rows: [...entities, ...state.rows],
        total: state.total + entities.length
      };
    },
    [removeEntity]: (state, entity) => {
      return {
        ...state,
        rows: _.reject(state.rows, entity),
        total: state.total - 1
      };
    },
    [updateState]: (state, newState) => {
      return {
        ...state,
        ...newState
      };
    },
    [updateItems]: (state, items) => {
      return {
        ...state,
        rows: _updateItems(state.rows, items)
      };
    },
    [resetSearch]: state => {
      return {
        ...state,
        ...initialState
      };
    }
  }, initialState);

  return {
    reducer,
    fetch,
    updateStateAndFetch,
    searchStart,
    refreshStart,
    searchSuccess,
    searchFailure,
    updateState,
    addEntity,
    addEntities,
    removeEntity,
    resetSearch,
    updateItems,
  };
}

export default makePagination;
