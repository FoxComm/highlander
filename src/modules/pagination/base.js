import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { updateItems as _updateItems } from '../state-helpers';

export const DEFAULT_PAGE_SIZE = 50;

export const DEFAULT_PAGE_SIZES = [
  ['25', 'View 25'],
  ['50', 'View 50'],
  ['75', 'View 75'],
  ['100', 'View 100'],
];

const initialState = {
  isFetching: null,
  rows: [],
  total: 0,
  from: 0,
  size: DEFAULT_PAGE_SIZE
};

export default function makePagination(namespace, fetcher) {

  const _createAction = (description, ...args) => {
    const name = `${namespace}_${description}`.toUpperCase();
    return createAction(name, ...args);
  };

  const submitSearch = _createAction('SUBMIT_SEARCH');
  const searchSuccess = _createAction('SEARCH_SUCCESS');
  const searchFailure = _createAction('SEARCH_FAILURE');
  const updateState = _createAction('UPDATE_STATE');
  const addEntity = _createAction('ADD_ENTITY');
  const addEntities = _createAction('ADD_ENTITIES');
  const removeEntity = _createAction('REMOVE_ENTITY');
  const reset = _createAction('RESET');
  const updateItems = _createAction('UPDATE_ITEMS');

  const fetch = (...args) => (dispatch, getState) => {
    const state = _.get(getState(), namespace);

    dispatch(submitSearch());

    return fetcher(...args, state)
      .then(
        result => dispatch(searchSuccess(result)),
        err => dispatch(searchFailure(err))
      );
  };

  const setFetchParams = (newState, ...args) => {
    return dispatch => {
      dispatch(updateState(newState));
      dispatch(fetch(...args));
    };
  };

  const reducer = createReducer({
    [submitSearch]: state => {
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
    [reset]: state => {
      return {
        ...state,
        ...initialState
      };
    }
  }, initialState);

  return {
    reducer,
    fetch,
    setFetchParams,
    submitSearch,
    searchSuccess,
    searchFailure,
    updateState,
    addEntity,
    addEntities,
    removeEntity,
    reset,
    updateItems,
  };
}
