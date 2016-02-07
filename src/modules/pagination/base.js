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

const initialState = {
  // isFetching = null, - fetching wasn't started yet
  // isFetching = true, - fetching was started
  // isFetching = false, - fetching was finished
  isFetching: null,
  rows: [],
  total: 0,
  from: 0,
  size: DEFAULT_PAGE_SIZE
};

export function makeFetchAction(fetcher, actions, findSearchState) {
  let fetchPromise;

  return (...args) => (dispatch, getState) => {
    const searchState = findSearchState(getState());

    if (!searchState.isFetching) {
      dispatch(actions.searchStart());

      fetchPromise = fetcher(...args, {searchState, getState})
        .then(
          result => dispatch(actions.searchSuccess(result)),
          err => dispatch(actions.searchFailure(err))
        );
    }

    return fetchPromise;
  };
}

export default function makePagination(namespace, fetcher) {

  const _createAction = (...args) => {
    return createNsAction(namespace, ...args);
  };

  const searchStart = _createAction('SEARCH_START');
  const searchSuccess = _createAction('SEARCH_SUCCESS');
  const searchFailure = _createAction('SEARCH_FAILURE');
  const updateState = _createAction('UPDATE_STATE');
  const addEntity = _createAction('ADD_ENTITY');
  const addEntities = _createAction('ADD_ENTITIES');
  const removeEntity = _createAction('REMOVE_ENTITY');
  const resetSearch = _createAction('RESET_SEARCH');
  const updateItems = _createAction('UPDATE_ITEMS');

  const fetch = makeFetchAction(fetcher, {searchStart, searchSuccess, searchFailure}, state => _.get(state, namespace));

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
        isFetching: true
      };
    },
    [searchSuccess]: (state, response) => {
      return {
        ...state,
        isFetching: false,
        rows: _.get(response, 'result', response),
        total: _.get(response, ['pagination', 'total'], response.length)
      };
    },
    [searchFailure]: (state, err) => {
      console.error(err);

      return {
        ...state,
        isFetching: false
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
