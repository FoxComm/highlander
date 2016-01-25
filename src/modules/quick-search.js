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

export default function makeQuickSearch(namespace, searchFilters, searchPhrase) {
  const searchStart = _createAction(namespace, 'SEARCH_START');
  const searchSuccess = _createAction(namespace, 'SEARCH_SUCCESS');
  const searchFailure = _createAction(namespace, 'SEARCH_FAILURE');
  const submitSearch = _createAction(namespace, 'SUBMIT_SEARCH');

  const doSearch = (url, filters, phrase) => {
    return dispatch => {
      dispatch(submitSearch(filters, phrase));
      const esQuery = toQuery(filters, phrase);
      dispatch(fetch(url, esQuery.toJSON()));
    };
  };

  const fetch = (url, ...args) => {
    return dispatch => {
      dispatch(searchStart());
      return post(url, ...args)
        .then(
          res => dispatch(searchSuccess(res)),
          err => dispatch(searchFailure(err, fetch))
        );
    };
  };

  const filters = searchFilters.map(st => new SearchTerm(st));
  const phrase = searchPhrase;
  const initialState = {
    quickSearch: { 
        ...emptyState,
        filters: filters,
        phrase: phrase
    }
  };

  const reducer = createReducer({
    [searchStart]: (state) => _searchStart(state),
    [searchSuccess]: (state, res) => _searchSuccess(state, res),
    [searchFailure]: (state, [err, source]) => _searchFailure(state, [err, source]),
    [submitSearch]: (state, filters, phrase) => _submitSearch(state, filters, phrase)
  }, initialState);

  return {
    reducer: reducer,
    actions: {
      doSearch,
      fetch,
      searchStart,
      searchSuccess,
      searchFailure,
      submitSearch
    }
  };
}

function _searchStart(state) {
  return assoc(state, ['quickSearch', 'isFetching'], true);
}

function _searchSuccess(state, res) {
  // Needed because nginx returns an array when result is found and an object otherwise.
  const results = _.isEmpty(res.result) ? [] : res.result;
  const total = get(res, ['pagination', 'total'], 0);

  return assoc(state,
    ['quickSearch', 'isFetching'], false,
    ['quickSearch', 'result', 'rows'], results,
    ['quickSearch', 'result', 'from'], 0,
    ['quickSearch', 'result', 'size'], total,
    ['quickSearch', 'result', 'total'], total
  );
}

function _searchFailure(state, [err, source]) {
  if (source === fetch) {
    console.error(err);
    return assoc(state, ['quickSearch', 'isFetching'], false);
  }

  return state;
}

function _submitSearch(state, filters, phrase) {
  return assoc(state, 
      ['quickSearch', 'filters'], filters,
      ['quickSearch', 'phrase'], phrase);
}
