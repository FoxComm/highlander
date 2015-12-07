import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import SearchTerm from '../paragons/search-term';
import util from 'util';

function deleteSearchFilter(state, idx) {
  if (!_.isEmpty(state.searches) && _.isEmpty(state.searchValue)) {
    return {
      ...state,
      searches: _.without(state.searches, state.searches[idx])
    };
  }

  return state;
}

function goBack(state) {
  const lastColonIdx = _.trim(state.searchValue, ' :').lastIndexOf(':');

  let newSearchTerm = null;
  if (lastColonIdx > 0) {
    newSearchTerm = `${state.searchValue.slice(0, lastColonIdx - 1)} : `;
  } else {
    newSearchTerm = '';
  }

  return submitFilter(state, newSearchTerm);
}

function submitFilter(state, searchTerm) {
  // First update the available terms.
  let searches = state.searches;
  let newSearchTerm = searchTerm;
  let options = SearchTerm.potentialTerms(state.potentialOptions, searchTerm);

  // Second, if there is only one term, see if we can turn it into a saved search.
  if (options.length == 1 && options[0].selectTerm(searchTerm)) {
    newSearchTerm = '';
    options = SearchTerm.potentialTerms(state.potentialOptions, newSearchTerm);
    searches = [...state.searches, searchTerm];
  }

  // Third, update the state.
  return {
    ...state,
    currentOptions: options,
    searches: searches,
    searchValue: newSearchTerm
  };
}

function liveSearchReducer(actionTypes, searchTerms) {
  const terms = searchTerms.map(st => new SearchTerm(st));

  const initialState = {
    currentOptions: terms,
    potentialOptions: terms,
    selectedIndex: -1,
    searches: [],
    searchValue: ''
  };

  return (state = initialState, action) => { 
    const payload = action.payload;

    switch (action.type) {
      case actionTypes.DELETE_SEARCH_FILTER:
        return deleteSearchFilter(state, payload.idx);

      case actionTypes.GO_BACK:
        return goBack(state);

      case actionTypes.SUBMIT_FILTER:
        return submitFilter(state, payload.searchTerm);

      default:
        return state;
    }
  };
}

function createLiveSearchActionTypes(namespace) {
  const actionTypes = [
    'DELETE_SEARCH_FILTER',
    'GO_BACK',
    'SUBMIT_FILTER'
  ];

  return _.transform(actionTypes, (result, type) => {
    const name = `${namespace.toUpperCase()}_${type}`;
    result[type] = name;
  });
}

function _createAction(namespace, description, ...args) {
  const name = `${namespace.toUpperCase()}_${description}`;
  return createAction(name, ...args);
}

function createLiveSearchActions(namespace) {
  return {
    deleteSearchFilter: _createAction(namespace, 'DELETE_SEARCH_FILTER', (idx) => ({idx})),
    goBack: _createAction(namespace, 'GO_BACK'),
    submitFilter: _createAction(namespace, 'SUBMIT_FILTER', (searchTerm) => ({searchTerm})),
  };
}

export default function makeLiveSearch(namespace, searchOptions) {
  const actions = createLiveSearchActions(namespace);
  const actionTypes = createLiveSearchActionTypes(namespace);
  const reducer = liveSearchReducer(actionTypes, searchOptions);
  return { reducer: reducer, actions: actions };
}
