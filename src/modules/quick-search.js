import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { assoc, get } from 'sprout-data';
import { post } from '../lib/search';
import { toQuery } from '../elastic/common';
import SearchTerm from '../paragons/search-term';

const emptyState = {
  isDirty: false,
  isFetching: false,
  isNew: false,
  result: {
    from: 0,
    size: 0,
    total: 0,
    rows: []
  },
  filters: [],
  phrase: ""
};

function _createAction(namespace, description, ...args) {
  const name = `${namespace}_${description}`.toUpperCase();
  return createAction(name, ...args);
}

export default function makeQuickSearch(namespace, searchUrl, searchFilters, searchPhrase) {
  const searchSuccess = _createAction(namespace, 'SEARCH_SUCCESS');
  const searchFailure = _createAction(namespace, 'SEARCH_FAILURE', (err, source) => [err, source]);
  const submitSearch = _createAction(namespace, 'SUBMIT_SEARCH', (filters, phrase) => [filters, phrase]);
  const clearSearch = _createAction(namespace, 'CLEAR_SEARCH');

  const url = searchUrl;
  const filters = searchFilters.map(st => new SearchTerm(st));
  const phrase = searchPhrase;
  const initialState = {
    ...emptyState,
    filters: filters,
    phrase: phrase
  };

  const doSearch = (phrase) => {
    return dispatch => {
      dispatch(submitSearch(filters, phrase));
      const esQuery = toQuery(filters, {phrase: phrase});
      dispatch(fetch(url, esQuery.toJSON()));
    };
  };

  const fetch = (url, ...args) => {
    return dispatch => {
      return post(url, ...args)
        .then(
          res => dispatch(searchSuccess(res)),
          err => dispatch(searchFailure(err, fetch))
        );
    };
  };

  const reducer = createReducer({
    [searchSuccess]: (state, res) => _searchSuccess(state, res),
    [searchFailure]: (state, [err, source]) => _searchFailure(state, [err, source]),
    [submitSearch]: (state, [filters, phrase]) => _submitSearch(state, filters, phrase),
    [clearSearch]: (state) => _clearSearch(state)
  }, initialState);

  return {
    reducer: reducer,
    actions: {
      doSearch,
      fetch,
      searchSuccess,
      searchFailure,
      submitSearch,
      clearSearch
    }
  };
}

function _searchSuccess(state, res) {
  // Needed because nginx returns an array when result is found and an object otherwise.
  const results = _.isEmpty(res.result) ? [] : res.result;
  const total = get(res, ['pagination', 'total'], 0);

  return assoc(state,
    ['isFetching'], false,
    ['result', 'rows'], results,
    ['result', 'from'], 0,
    ['result', 'size'], total,
    ['result', 'total'], total
  );
}

function _searchFailure(state, [err, source]) {
  if (source === fetch) {
    console.error(err);
    return assoc(state, ['isFetching'], false);
  }

  return state;
}

function _submitSearch(state, filters, phrase) {
  return assoc(state, 
      ['isFetching'], true,
      ['filters'], filters,
      ['phrase'], phrase);
}

function _clearSearch(state) {
  return assoc(state, 'phrase', "");
}
