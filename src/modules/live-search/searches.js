import _ from 'lodash';
import { createReducer } from 'redux-act';
import { assoc } from 'sprout-data';
import Api from '../../lib/api';
import SearchTerm from '../../paragons/search-term';
import { createNsAction } from './../utils';

const emptyState = {
  isDirty: false,
  isNew: false,
  isSaving: false,
  isEditable: true,
  isUpdating: false,
  options: [],
  phrase: null,
  results: void 0,
  query: [],
  searchValue: '',
  selectedIndex: -1
};

// module is responsible for search tabs

export default function makeSearches(namespace, fetch, searchTerms, scope, options = {}) {
  const { skipInitialFetch = false } = options;

  // Methods internal to the live search module
  const saveSearchStart = createNsAction(namespace, 'SAVE_SEARCH_START');
  const saveSearchSuccess = createNsAction(namespace, 'SAVE_SEARCH_SUCCESS');
  const saveSearchFailure = createNsAction(namespace, 'SAVE_SEARCH_FAILURE');
  const updateSearchStart = createNsAction(namespace, 'UPDATE_SEARCH_START');
  const updateSearchSuccess = createNsAction(namespace, 'UPDATE_SEARCH_SUCCESS', (idx, payload) => [idx, payload]);
  const updateSearchFailure = createNsAction(namespace, 'UPDATE_SEARCH_FAILURE', (idx, err) => [idx, err]);
  const deleteSearchStart = createNsAction(namespace, 'DELETE_SEARCH_START');
  const deleteSearchSuccess = createNsAction(namespace, 'DELETE_SEARCH_SUCCESS');
  const deleteSearchFailure = createNsAction(namespace, 'DELETE_SEARCH_FAILURE', (idx, err) => [idx, err]);

  const selectSavedSearch = createNsAction(namespace, 'SELECT_SAVED_SEARCH');
  const submitFilters = createNsAction(namespace, 'SUBMIT_FILTERS');
  const submitPhrase = createNsAction(namespace, 'SUBMIT_PHRASE');
  const fetchSearchesStart = createNsAction(namespace, 'FETCH_SEARCHES_START');
  const fetchSearchesSuccess = createNsAction(namespace, 'FETCH_SEARCHES_SUCCESS');
  const fetchSearchesFailure = createNsAction(namespace, 'FETCH_SEARCHES_FAILURE');

  const setSearchTerms = createNsAction(namespace, 'SET_SEARCH_TERMS');

  const addSearchFilters = (filters, initial = false) => {
    return dispatch => {
      dispatch(submitFilters(filters));
      if (!initial || !skipInitialFetch) {
        dispatch(fetch());
      }
    };
  };

  const addSearchPhrase = phrase => {
    return dispatch => {
      dispatch(submitPhrase(phrase));
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
    return dispatch => {
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

  const initialState = _setSearchTerms({
    updateNum: 0,
    isSavingSearch: false,
    fetchingSearches: false,
    selectedSearch: 0,
    savedSearches: [
      {
        ...emptyState,
        title: 'All',
        isEditable: false
      }
    ],
    currentSearch() {
      return this.savedSearches[this.selectedSearch];
    }
  }, searchTerms);

  const reducer = createReducer({
    [saveSearchStart]: (state) => _saveSearchStart(state),
    [saveSearchSuccess]: (state, payload) => _saveSearchSuccess(state, payload),
    [saveSearchFailure]: (state, err) => _saveSearchFailure(state, err),
    [selectSavedSearch]: (state, idx) => _selectSavedSearch(state, idx),
    [submitFilters]: (state, filters) => _submitFilters(state, filters),
    [submitPhrase]: (state, phrase) => _submitPhrase(state, phrase),
    [fetchSearchesStart]: (state) => _fetchSearchesStart(state),
    [fetchSearchesSuccess]: (state, searches) => _fetchSearchesSuccess(state, searches),
    [fetchSearchesFailure]: (state, err) => _fetchSearchesFailure(state, err),
    [updateSearchStart]: (state, idx) => _updateSearchStart(state, idx),
    [updateSearchSuccess]: (state, [idx, payload]) => _updateSearchSuccess(state, [idx, payload]),
    [updateSearchFailure]: (state, [idx, err]) => _updateSearchFailure(state, [idx, err]),
    [deleteSearchStart]: (state, idx) => _deleteSearchStart(state, idx),
    [deleteSearchSuccess]: (state, idx) => _deleteSearchSuccess(state, idx),
    [deleteSearchFailure]: (state, [idx, err]) => _deleteSearchFailure(state, [idx, err]),
    [setSearchTerms]: (state, searchTerms) => _setSearchTerms(state, searchTerms),
  }, initialState);

  return {
    reducer,
    actions: {
      addSearchFilters,
      addSearchPhrase,
      fetch,
      fetchSearches,
      saveSearch,
      selectSearch,
      selectSavedSearch,
      submitFilters,
      updateSearch,
      deleteSearch,
      setSearchTerms,
    }
  };
}

function _setSearchTerms(state, searchTerms) {
  const terms = searchTerms.map(st => new SearchTerm(st));

  return assoc(state, 'searchOptions', terms);
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

function _submitPhrase(state, phrase) {
  return assoc(state,
    ['savedSearches', state.selectedSearch, 'phrase'], phrase,
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
