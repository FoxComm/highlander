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
  options: [],
  results: {
    from: 0,
    size: 0,
    total: 0,
    rows: []
  },
  searches: [],
  searchValue: '',
  selectedIndex: -1
};

function _createAction(namespace, description, ...args) {
  const name = `${namespace}_${description}`.toUpperCase();
  return createAction(name, ...args);
}

export default function makeLiveSearch(namespace, searchTerms, scope) {
  const internalNS = namespace.toUpperCase();

  // Methods internal to the live search module
  const saveSearchStart = _createAction(internalNS, 'SAVE_SEARCH_START');
  const saveSearchSuccess = _createAction(internalNS, 'SAVE_SEARCH_SUCCESS');
  const saveSearchFailure = _createAction(internalNS, 'SAVE_SEARCH_FAILURE');

  const searchStart = _createAction(internalNS, 'SEARCH_START');
  const searchSuccess = _createAction(internalNS, 'SEARCH_SUCCESS');
  const searchFailure = _createAction(internalNS, 'SEARCH_FAILURE');
  const selectSavedSearch = _createAction(internalNS, 'SELECT_SAVED_SEARCH');
  const submitFilters = _createAction(internalNS, 'SUBMIT_FILTER');
  const fetchSearchesStart = _createAction(internalNS, 'FETCH_SEARCHES_START');
  const fetchSearchesSuccess = _createAction(internalNS, 'FETCH_SEARCHES_SUCCESS');
  const fetchSearchesFailure = _createAction(internalNS, 'FETCH_SEARCHES_FAILURE');

  const addSearchFilter = (url, filters) => {
    return dispatch => {
      dispatch(submitFilters(filters));
      const esQuery = toQuery(filters);
      dispatch(fetch(url, esQuery.toJSON()));
    };
  };

  const getSelectedSearch = (state) => {
    const selectedSearch = _.get(state, [namespace, 'list', 'selectedSearch']);
    return _.get(state, [namespace, 'list', 'savedSearches', selectedSearch]);
  };

  const fetch = (url, ...args) => {
    let fetchPromise;

    return (dispatch, getState) => {
      const { isFetching } = getSelectedSearch(getState());

      if (!isFetching) {
        dispatch(searchStart());
        fetchPromise = post(url, ...args)
          .then(
            res => dispatch(searchSuccess(res)),
            err => dispatch(searchFailure(err, fetch))
          );
      }

      return fetchPromise;
    };
  };

  const fetchSearches = () => {
    return dispatch => {
      dispatch(fetchSearchesStart());
      return Api.get(`/shared-search?=${scope}`)
        .then(
          searches => dispatch(fetchSearchesSuccess(searches)),
          err => dispatch(fetchSearchesFailure(err, fetchSearches))
        );
      };
  };

  const saveSearch = search => {
    const payload = {
      title: search.name,
      query: search.searches,
      scope: scope
    };

    return dispatch => {
      dispatch(saveSearchStart());
      return Api.post('/shared-search', payload)
        .then(
          search => dispatch(saveSearchSuccess(search)),
          err => dispatch(saveSearchFailure(err, saveSearch))
        );
    };
  };

  const selectSearch = (url, idx) => {
    return (dispatch, getState) => {
      dispatch(selectSavedSearch(idx));

      const searchTerms = _.get(getSelectedSearch(getState()), 'searches', []);

      const esQuery = toQuery(searchTerms);
      dispatch(fetch(url, esQuery.toJSON()));
    };
  };

  const terms = searchTerms.map(st => new SearchTerm(st));
  const initialState = {
    isSavingSearch: false,
    fetchingSearches: false,
    potentialOptions: terms,
    selectedSearch: 0,
    savedSearches: [
      {
        ...emptyState,
        name: 'All',
        currentOptions: terms
      }
    ]
  };

  const reducer = createReducer({
    [saveSearchStart]: (state, idx) => _saveSearchStart(state),
    [saveSearchSuccess]: (state, payload) => _saveSearchSuccess(state, payload),
    [saveSearchFailure]: (state, [err, source]) => _saveSearchFailure(state, [err, source]),
    [searchStart]: (state) => _searchStart(state),
    [searchSuccess]: (state, res) => _searchSuccess(state, res),
    [searchFailure]: (state, [err, source]) => _searchFailure(state, [err, source]),
    [selectSavedSearch]: (state, idx) => _selectSavedSearch(state, idx),
    [submitFilters]: (state, filters) => _submitFilters(state, filters),
    [fetchSearchesStart]: (state) => _fetchSearchesStart(state),
    [fetchSearchesSuccess]: (state, searches) => _fetchSearchesSuccess(state, searches),
    [fetchSearchesFailure]: (state, [err, source]) => _fetchSearchesFailure(state, [err, source])
}, initialState);

  return {
    reducer: reducer,
    actions: {
      addSearchFilter,
      fetch,
      fetchSearches,
      saveSearch,
      searchStart,
      searchSuccess,
      searchFailure,
      selectSearch,
      selectSavedSearch,
      submitFilters,
      fetchSearchesStart,
      fetchSearchesSuccess,
      fetchSearchesFailure
    }
  };
}

function _saveSearchStart(state) {
  return assoc(state, 'isSavingSearch', true);
}

function _saveSearchSuccess(state, payload) {
  const searches = { ...state.savedSearches, payload };
  return assoc(state,
    ['savedSearches'], searches,
    ['selectedSearch'], searches.length - 1
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

function _searchFailure(state, [err, source]) {
  if (source === fetch) {
    console.error(err);
    return assoc(state, ['savedSearches', state.selectedSearch, 'isFetching'], false);
  }

  return state;
}

function _submitFilters(state, filters) {
  return assoc(state, ['savedSearches', state.selectedSearch, 'searches'], filters);
}

function _fetchSearchesStart(state) {
  return assoc(state, 'fetchingSearches', true);
}

function _fetchSearchesSuccess(state, searches) {
  return assoc(state, 'fetchingSearches', false);
}

function _fetchSearchesFailure(state, [err, source]) {
  if (source == fetchSuccess) {
    console.error(err);
    return assoc(state, 'fetchingSearches', false);
  }
  return assoc(state, 'fetchingSearches', false);
}
