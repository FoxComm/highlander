import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { assoc } from 'sprout-data';
import Api from '../../lib/api';
import SearchTerm from '../../paragons/search-term';

const emptyState = {
  isDirty: false,
  isNew: false,
  isSaving: false,
  isEditable: true,
  isUpdating: false,
  options: [],
  results: void 0,
  query: [],
  searchValue: '',
  selectedIndex: -1
};

function _createAction(namespace, description, ...args) {
  const name = `${namespace}_${description}`.toUpperCase();
  return createAction(name, ...args);
}

// module is responsible for search tabs

export default function makeSearches(namespace, fetch, searchTerms, scope) {
  // Methods internal to the live search module
  const saveSearchStart = _createAction(namespace, 'SAVE_SEARCH_START');
  const saveSearchSuccess = _createAction(namespace, 'SAVE_SEARCH_SUCCESS');
  const saveSearchFailure = _createAction(namespace, 'SAVE_SEARCH_FAILURE');
  const updateSearchStart = _createAction(namespace, 'UPDATE_SEARCH_START');
  const updateSearchSuccess = _createAction(namespace, 'UPDATE_SEARCH_SUCCESS', (idx, payload) => [idx, payload]);
  const updateSearchFailure = _createAction(namespace, 'UPDATE_SEARCH_FAILURE', (idx, err) => [idx, err]);
  const deleteSearchStart = _createAction(namespace, 'DELETE_SEARCH_START');
  const deleteSearchSuccess = _createAction(namespace, 'DELETE_SEARCH_SUCCESS');
  const deleteSearchFailure = _createAction(namespace, 'DELETE_SEARCH_FAILURE', (idx, err) => [idx, err]);

  const selectSavedSearch = _createAction(namespace, 'SELECT_SAVED_SEARCH');
  const submitFilters = _createAction(namespace, 'SUBMIT_FILTERS');
  const fetchSearchesStart = _createAction(namespace, 'FETCH_SEARCHES_START');
  const fetchSearchesSuccess = _createAction(namespace, 'FETCH_SEARCHES_SUCCESS');
  const fetchSearchesFailure = _createAction(namespace, 'FETCH_SEARCHES_FAILURE');

  const addSearchFilters = filters => {
    return dispatch => {
      dispatch(submitFilters(filters));
      dispatch(fetch());
    };
  };

  const fetchSearches = () => {
    return dispatch => {
      dispatch(fetchSearchesStart());
      return Api.get(`/shared-search?scope=${scope}`)
        .then(
          searches => dispatch(fetchSearchesSuccess(searches)),
          err => dispatch(fetchSearchesFailure(err))
        );
    };
  };

  const saveSearch = search => {
    const payload = {
      title: search.title,
      query: search.query,
      scope: scope
    };

    return dispatch => {
      dispatch(saveSearchStart());
      return Api.post('/shared-search', payload)
        .then(
          search => {
            dispatch(saveSearchSuccess(search));
            dispatch(fetch());
          },
          err => dispatch(saveSearchFailure(err))
        );
    };
  };

  const selectSearch = idx => {
    return (dispatch, getState) => {
      dispatch(selectSavedSearch(idx));
      dispatch(fetch());
    };
  };

  const updateSearch = (idx, search) => {
    const payload = {
      title: search.title,
      query: search.query,
      scope: scope
    };

    return dispatch => {
      dispatch(updateSearchStart(idx));
      return Api.patch(`/shared-search/${search.code}`, payload)
        .then(
          search => {
            dispatch(updateSearchSuccess(idx, search));
            dispatch(fetch());
          },
          err => dispatch(updateSearchFailure(idx, err))
        );
    };
  };

  const deleteSearch = (idx, search) => {
    return dispatch => {
      dispatch(deleteSearchStart(idx));
      return Api.delete(`/shared-search/${search.code}`)
        .then(
          resp => dispatch(deleteSearchSuccess(idx)),
          err => dispatch(deleteSearchFailure(idx, err))
        );
    };
  };

  const terms = searchTerms.map(st => new SearchTerm(st));
  const initialState = {
    updateNum: 0,
    isSavingSearch: false,
    fetchingSearches: false,
    searchOptions: terms,
    selectedSearch: 0,
    savedSearches: [
      {
        ...emptyState,
        title: 'All',
        isEditable: false
      }
    ]
  };

  const reducer = createReducer({
    [saveSearchStart]: (state) => _saveSearchStart(state),
    [saveSearchSuccess]: (state, payload) => _saveSearchSuccess(state, payload),
    [saveSearchFailure]: (state, err) => _saveSearchFailure(state, err),
    [selectSavedSearch]: (state, idx) => _selectSavedSearch(state, idx),
    [submitFilters]: (state, filters) => _submitFilters(state, filters),
    [fetchSearchesStart]: (state) => _fetchSearchesStart(state),
    [fetchSearchesSuccess]: (state, searches) => _fetchSearchesSuccess(state, searches),
    [fetchSearchesFailure]: (state, err) => _fetchSearchesFailure(state, err),
    [updateSearchStart]: (state, idx) => _updateSearchStart(state, idx),
    [updateSearchSuccess]: (state, [idx, payload]) => _updateSearchSuccess(state, [idx, payload]),
    [updateSearchFailure]: (state, [idx, err]) => _updateSearchFailure(state, [idx, err]),
    [deleteSearchStart]: (state, idx) => _deleteSearchStart(state, idx),
    [deleteSearchSuccess]: (state, idx) => _deleteSearchSuccess(state, idx),
    [deleteSearchFailure]: (state, [idx, err]) => _deleteSearchFailure(state, [idx, err]),
  }, initialState);

  return {
    reducer,
    actions: {
      addSearchFilters,
      fetch,
      fetchSearches,
      saveSearch,
      selectSearch,
      selectSavedSearch,
      submitFilters,
      updateSearch,
      deleteSearch,
    }
  };
}

function _saveSearchStart(state) {
  return assoc(state, 'isSavingSearch', true);
}

function _saveSearchSuccess(state, payload) {
  const search = { ...emptyState, ...payload };
  const searches = [ ...state.savedSearches, search ];

  return assoc(state,
    ['savedSearches'], searches,
    ['selectedSearch'], searches.length - 1,
    'isSavingSearch', false
  );
}

function _saveSearchFailure(state, err) {
  console.error(err);
  return assoc(state, 'isSavingSearch', false);
}

function _selectSavedSearch(state, idx) {
  if (idx > -1 && idx < state.savedSearches.length) {
    return assoc(state,
      ['selectedSearch'], idx,
      ['savedSearches', state.selectedSearch, 'isEditingName'], false
    );
  }

  return state;
}

function _submitFilters(state, filters) {
  return assoc(state,
    ['savedSearches', state.selectedSearch, 'query'], filters,
    ['savedSearches', state.selectedSearch, 'isDirty'], true
  );
}

function _fetchSearchesStart(state) {
  return assoc(state, 'fetchingSearches', true);
}

function _fetchSearchesSuccess(state, searches) {
  const mappedSearches = searches.map(search => {
    return { ...emptyState, ...search };
  });

  return assoc(state,
    'fetchingSearches', false,
    'savedSearches', [...state.savedSearches, ...mappedSearches]
  );
}

function _fetchSearchesFailure(state, err) {
  console.error(err);
  return assoc(state, 'fetchingSearches', false);
}

function _updateSearchStart(state, idx) {
  return assoc(state, ['savedSearches', idx, 'isUpdating'], true);
}

function _updateSearchSuccess(state, [idx, payload]) {
  return assoc(state,
    ['savedSearches', idx], { ...emptyState, ...payload },
    'updateNum', state.updateNum + 1);
}

function _updateSearchFailure(state, [idx, err]) {
  console.error(err);
  return assoc(state, ['savedSearches', idx, 'isUpdating'], false);
}

function _deleteSearchStart(state, idx) {
  return assoc(state, ['savedSearches', idx, 'isDeleting'], true);
}

function _deleteSearchSuccess(state, idx) {
  const searches = [
    ...state.savedSearches.slice(0, idx),
    ...state.savedSearches.slice(idx + 1)
  ];

  const selectedSearch = idx == state.selectedSearch
    ? state.selectedSearch - 1
    : state.selectedSearch;

  return assoc(state,
    'savedSearches', searches,
    'selectedSearch', selectedSearch
  );
}

function _deleteSearchFailure(state, [idx, err]) {
  console.error(err);
  return assoc(state, ['savedSearches', idx, 'isDeleting'], false);
}
