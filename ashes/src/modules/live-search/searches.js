import _ from 'lodash';
import { createReducer } from 'redux-act';
import reduceReducers from 'reduce-reducers';
import { assoc } from 'sprout-data';
import Api from '../../lib/api';
import SearchTerm from '../../paragons/search-term';
import { createNsAction } from './../utils';
import makeAssociations from './searches-associations';
import { toQuery } from '../../elastic/common';

const emptyState = {
  isDirty: false,
  isNew: false,
  isSaving: false,
  isEditable: true,
  isUpdating: false,
  isEditing: false,
  options: [],
  results: void 0,
  query: [],
  searchValue: '',
  shares: {
    associations: []
  }
};

function isExistsQueryContext(filters) {
  return _.some(filters, filter => {
    return filter.value.type === 'string';
  });
}

// module is responsible for search tabs

export default function makeSearches(namespace, dataActions, searchTerms, scope, options = {}) {
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
  const submitFilters = createNsAction(namespace, 'SUBMIT_FILTERS', (filters, initial) => [filters, initial]);
  const submitPhrase = createNsAction(namespace, 'SUBMIT_PHRASE');
  const fetchSearchesStart = createNsAction(namespace, 'FETCH_SEARCHES_START');
  const fetchSearchesSuccess = createNsAction(namespace, 'FETCH_SEARCHES_SUCCESS');
  const fetchSearchesFailure = createNsAction(namespace, 'FETCH_SEARCHES_FAILURE');

  const setSearchTerms = createNsAction(namespace, 'SET_SEARCH_TERMS');

  const addSearchFilters = (filters, initial = false) => {
    return dispatch => {
      const searchState = { from: 0 };
      if (isExistsQueryContext(filters)) {
        searchState.sortBy = null;
      }
      dispatch(submitFilters(filters, initial));
      dispatch(dataActions.updateState(searchState));
      if (!initial || !skipInitialFetch) {
        dispatch(dataActions.fetch());
      }
    };
  };

  const addSearchPhrase = phrase => {
    return dispatch => {
      // in case of phrase search drop sorting in order to pass sorting to server
      dispatch(dataActions.updateState({ from: 0, sortBy: null }));
      dispatch(submitPhrase(phrase));
      dispatch(dataActions.fetch());
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
      scope: scope,
      rawQuery: toQuery(search.query),
    };

    return dispatch => {
      dispatch(saveSearchStart());
      return Api.post('/shared-search', payload)
        .then(
          search => {
            dispatch(saveSearchSuccess(search));
            dispatch(dataActions.fetch());
          },
          err => dispatch(saveSearchFailure(err))
        );
    };
  };

  const selectSearch = idx => {
    return dispatch => {
      dispatch(selectSavedSearch(idx));
      dispatch(dataActions.fetch());
    };
  };

  const updateSearch = (idx, search) => {
    const payload = {
      title: search.title,
      query: search.query,
      scope: scope,
      rawQuery: toQuery(search.query),
    };

    return dispatch => {
      dispatch(updateSearchStart(idx));
      return Api.patch(`/shared-search/${search.code}`, payload)
        .then(
          search => {
            dispatch(updateSearchSuccess(idx, search));
            dispatch(dataActions.fetch());
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

  const searchesReducer = createReducer({
    [dataActions.searchStart]: (state) => _fetchSearchStart(state),
    [saveSearchStart]: (state) => _saveSearchStart(state),
    [saveSearchSuccess]: (state, payload) => _saveSearchSuccess(state, payload),
    [saveSearchFailure]: (state, err) => _saveSearchFailure(state, err),
    [selectSavedSearch]: (state, idx) => _selectSavedSearch(state, idx),
    [submitFilters]: (state, [filters, initial]) => _submitFilters(state, filters, initial),
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

  /** generate reducer and actions for user associations management */
  const associations = makeAssociations(namespace);

  const reducer = reduceReducers(searchesReducer, associations.reducer);

  return {
    reducer,
    actions: {
      addSearchFilters,
      addSearchPhrase,
      fetch: dataActions.fetch,
      fetchSearches,
      saveSearch,
      selectSearch,
      selectSavedSearch,
      submitFilters,
      updateSearch,
      deleteSearch,
      setSearchTerms,
      ...associations.actions
    }
  };
}

function _fetchSearchStart(state) {
  /** reset isFetching for all searches on new search start */
  const mappedSearches = state.savedSearches.map((search, index) => {
    /** don't touch search if it's a selected search or its results are not initializes yet */
    if (state.selectedSearch === index || !search.results) {
      return search;
    }

    return assoc(search, ['results', 'isFetching'], false);
  });

  return assoc(state, 'savedSearches', mappedSearches);
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
  const searches = [...state.savedSearches, search];

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

function _submitFilters(state, filters, initial) {
  const wasDirty = _.get(state, ['savedSearches', state.selectedSearch, 'isDirty'], true);

  /** not mark search as dirty on initial filters set but left it dirty if it was marked before */
  const isDirty = !initial || wasDirty;

  return assoc(state,
    ['savedSearches', state.selectedSearch, 'query'], filters,
    ['savedSearches', state.selectedSearch, 'isDirty'], isDirty
  );
}

function _submitPhrase(state, phrase) {
  const filter = {
    display: phrase,
    term: '_all',
    operator: 'eq',
    value: { type: 'string', value: phrase },
  };

  const query = [...state.savedSearches[state.selectedSearch].query, filter];

  return assoc(state,
    ['savedSearches', state.selectedSearch, 'query'], query,
    ['savedSearches', state.selectedSearch, 'isDirty'], true
  );
}

function _fetchSearchesStart(state) {
  return assoc(state, 'fetchingSearches', true);
}

function _fetchSearchesSuccess(state, searches) {
  const mappedSearches = searches.map(search => ({ ...emptyState, ...search }));

  let updatedSearches = state.savedSearches.map(oldSearch => {
    /** left dirty searches untouched */
    if (oldSearch.isDirty || oldSearch.id === void 0) {
      return oldSearch;
    }

    for (const newSearch of mappedSearches) {
      /** update old search with new one from the server */
      if (newSearch.id === oldSearch.id) {
        return newSearch;
      }
    }

    /** old search not found in server response, delete it later */
    return void 0;
  })
    .filter(search => search !== void 0);

  const ids = updatedSearches.map(search => search.id);

  /** completely new searches received from server */
  const newSearches = mappedSearches.filter(search => ids.indexOf(search.id) === -1);

  return assoc(state,
    'fetchingSearches', false,
    'savedSearches', [...updatedSearches, ...newSearches]
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
  // keep shares array for updated search
  const shares = _.get(state, ['savedSearches', idx, 'shares']);

  return assoc(state,
    ['savedSearches', idx], { ...emptyState, ...payload },
    ['savedSearches', idx, 'shares'], shares,
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
