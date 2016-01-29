import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { assoc, get } from 'sprout-data';
import { post } from '../lib/search';
import Api from '../lib/api';
import { toQuery } from '../elastic/common';
import SearchTerm from '../paragons/search-term';

const emptyState = {
  isDirty: false,
  // isFetching = null, - fetching wasn't started yet
  // isFetching = true, - fetching was started
  // isFetching = false, - fetching was finished
  isFetching: null,
  isNew: false,
  isSaving: false,
  isEditable: true,
  isUpdating: false,
  options: [],
  results: {
    from: 0,
    size: 0,
    total: 0,
    rows: []
  },
  query: [],
  searchValue: '',
  selectedIndex: -1
};

function _createAction(namespace, description, ...args) {
  const name = `${namespace}_${description}`.toUpperCase();
  return createAction(name, ...args);
}

export default function makeLiveSearch(namespace, searchTerms, esUrl, scope) {
  // Methods internal to the live search module
  const saveSearchStart = _createAction(namespace, 'SAVE_SEARCH_START');
  const saveSearchSuccess = _createAction(namespace, 'SAVE_SEARCH_SUCCESS');
  const saveSearchFailure = _createAction(namespace, 'SAVE_SEARCH_FAILURE', (err, source) => [err, source]);
  const updateSearchStart = _createAction(namespace, 'UPDATE_SEARCH_START');
  const updateSearchSuccess = _createAction(namespace, 'UPDATE_SEARCH_SUCCESS', (idx, payload) => [idx, payload]);
  const updateSearchFailure = _createAction(namespace, 'UPDATE_SEARCH_FAILURE', (idx, err, source) => [idx, err, source]);
  const deleteSearchStart = _createAction(namespace, 'DELETE_SEARCH_START');
  const deleteSearchSuccess = _createAction(namespace, 'DELETE_SEARCH_SUCCESS');
  const deleteSearchFailure = _createAction(namespace, 'DELETE_SEARCH_FAILURE', (idx, err, source) => [idx, err, source]);

  const searchStart = _createAction(namespace, 'SEARCH_START');
  const searchSuccess = _createAction(namespace, 'SEARCH_SUCCESS');
  const searchFailure = _createAction(namespace, 'SEARCH_FAILURE');
  const selectSavedSearch = _createAction(namespace, 'SELECT_SAVED_SEARCH');
  const submitFilters = _createAction(namespace, 'SUBMIT_FILTERS');
  const fetchSearchesStart = _createAction(namespace, 'FETCH_SEARCHES_START');
  const fetchSearchesSuccess = _createAction(namespace, 'FETCH_SEARCHES_SUCCESS');
  const fetchSearchesFailure = _createAction(namespace, 'FETCH_SEARCHES_FAILURE');

  const addSearchFilters = filters => {
    return dispatch => {
      dispatch(submitFilters(filters));
      const esQuery = toQuery(filters);
      dispatch(fetch(esQuery.toJSON()));
    };
  };

  const getSelectedSearch = (state) => {
    const selectedSearch = _.get(state, [namespace, 'list', 'selectedSearch']);
    return _.get(state, [namespace, 'list', 'savedSearches', selectedSearch]);
  };

  const fetch = (...args) => {
    let fetchPromise;

    return (dispatch, getState) => {
      const isFetching = _.get(getSelectedSearch(getState()), 'isFetching');

      if (!isFetching) {
        dispatch(searchStart());
        fetchPromise = post(esUrl, ...args)
          .then(
            res => dispatch(searchSuccess(res)),
            err => dispatch(searchFailure(err))
          );
      }

      return fetchPromise;
    };
  };

  const fetchSearches = () => {
    return dispatch => {
      dispatch(fetchSearchesStart());
      return Api.get(`/shared-search?scope=${scope}`)
        .then(
          searches => dispatch(fetchSearchesSuccess(searches)),
          err => dispatch(fetchSearchesFailure(err, fetchSearches))
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
            dispatch(queryElasticSearch(esUrl));
          },
          err => dispatch(saveSearchFailure(err, saveSearch))
        );
    };
  };

  const selectSearch = idx => {
    return (dispatch, getState) => {
      dispatch(selectSavedSearch(idx));
      dispatch(queryElasticSearch(esUrl));
    };
  };

  const queryElasticSearch = () => {
    return (dispatch, getState) => {
      const searchTerms = _.get(getSelectedSearch(getState()), 'query', []);
      const esQuery = toQuery(searchTerms);
      dispatch(fetch(esQuery.toJSON()));
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
            dispatch(queryElasticSearch());
          },
          err => dispatch(updateSearchFailure(idx, err, updateSearch))
        );
    };
  };

  const deleteSearch = (idx, search) => {
    return dispatch => {
      dispatch(deleteSearchStart(idx));
      return Api.delete(`/shared-search/${search.code}`)
        .then(
          resp => dispatch(deleteSearchSuccess(idx)),
          err => dispatch(deleteSearchFailure(idx, err, deleteSearch))
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
    [saveSearchFailure]: (state, [err, source]) => _saveSearchFailure(state, [err, source]),
    [searchStart]: (state) => _searchStart(state),
    [searchSuccess]: (state, res) => _searchSuccess(state, res),
    [searchFailure]: (state, err) => _searchFailure(state, err),
    [selectSavedSearch]: (state, idx) => _selectSavedSearch(state, idx),
    [submitFilters]: (state, filters) => _submitFilters(state, filters),
    [fetchSearchesStart]: (state) => _fetchSearchesStart(state),
    [fetchSearchesSuccess]: (state, searches) => _fetchSearchesSuccess(state, searches),
    [fetchSearchesFailure]: (state, [err, source]) => _fetchSearchesFailure(state, [err, source]),
    [updateSearchStart]: (state, idx) => _updateSearchStart(state, idx),
    [updateSearchSuccess]: (state, [idx, payload]) => _updateSearchSuccess(state, [idx, payload]),
    [updateSearchFailure]: (state, [idx, err, source]) => _updateSearchFailure(state, [idx, err, source]),
    [deleteSearchStart]: (state, idx) => _deleteSearchStart(state, idx),
    [deleteSearchSuccess]: (state, idx) => _deleteSearchSuccess(state, idx),
    [deleteSearchFailure]: (state, [idx, err, source]) => _deleteSearchFailure(state, [idx, err, source]),
}, initialState);

  return {
    reducer: reducer,
    actions: {
      addSearchFilters,
      fetch,
      fetchSearches,
      saveSearch,
      searchStart,
      searchSuccess,
      searchFailure,
      selectSearch,
      selectSavedSearch,
      submitFilters,
      updateSearch,
      deleteSearch
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

function _saveSearchFailure(state, [err, source]) {
  if (source == saveSearch) {
    console.error(err);
    return assoc(state, 'isSavingSearch', false);
  }

  return state;
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

function _searchStart(state) {
  return assoc(state, ['savedSearches', state.selectedSearch, 'isFetching'], true);
}

function _searchSuccess(state, res) {
  // Needed because nginx returns an array when result is found and an object otherwise.
  const results = _.isEmpty(res.result) ? [] : res.result;
  const total = get(res, ['pagination', 'total'], 0);

  return assoc(state,
    ['savedSearches', state.selectedSearch, 'isFetching'], false,
    ['savedSearches', state.selectedSearch, 'results', 'rows'], results,
    ['savedSearches', state.selectedSearch, 'results', 'from'], 0,
    ['savedSearches', state.selectedSearch, 'results', 'size'], total,
    ['savedSearches', state.selectedSearch, 'results', 'total'], total
  );
}

function _searchFailure(state, err) {
  console.error(err);
  return assoc(state, ['savedSearches', state.selectedSearch, 'isFetching'], false);
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

function _fetchSearchesFailure(state, [err, source]) {
  if (source == fetchSuccess) {
    console.error(err);
    return assoc(state, 'fetchingSearches', false);
  }
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

function _updateSearchFailure(state, [idx, err, source]) {
  if (source == updateSearch) {
    console.error(err);
    return assoc(state, ['savedSearches', idx, 'isUpdating'], false);
  }

  return state;
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

function _deleteSearchFailure(state, [idx, err, source]) {
  if (source == deleteSearch) {
    console.error(err);
    return assoc(state, ['savedSearches', idx, 'isDeleting'], false);
  }

  return state;
}
