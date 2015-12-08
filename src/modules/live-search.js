import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import SearchTerm from '../paragons/search-term';
import util from 'util';
import { assoc } from 'sprout-data';

const emptyState = {
  options: [],
  searches: [],
  searchValue: '',
  selectedIndex: -1
};

function deleteSearchFilter(state, idx) {
  const curSearches = state.savedSearches[state.selectedSearch].searches;
  const curValue = state.savedSearches[state.selectedSearch].searchValue;

  if (!_.isEmpty(curSearches) && _.isEmpty(curValue)) {
    const updatedState = {
      ...state.savedSearches[state.selectedSearch],
      searches: newSearches
    };
    const newSearches = _.without(curSearches, curSearches[idx]);
    return assoc(state, ['savedSearches', state.selectedSearch], updatedState);
  }

  return state;
}

function goBack(state) {
  const curState = state.savedSearches[state.selectedSearch].searchValue;
  const lastColonIdx = _.trim(curState, ' :').lastIndexOf(':');
  const newSearchTerm = lastColonIdx > 0 ? `${curState.slice(0, lastColonIdx - 1)} : ` : '';
  return submitFilter(state, newSearchTerm);
}

function submitFilter(state, searchTerm) {
  // First update the available terms.
  let searches = state.savedSearches['All'].searches;
  let newSearchTerm = searchTerm;
  let options = SearchTerm.potentialTerms(state.potentialOptions, searchTerm);

  // Second, if there is only one term, see if we can turn it into a saved search.
  if (options.length == 1 && options[0].selectTerm(searchTerm)) {
    newSearchTerm = '';
    options = SearchTerm.potentialTerms(state.potentialOptions, newSearchTerm);
    searches = [...state.savedSearches[state.selectedSearch].searches, searchTerm];
  }

  // Third, update the state.
  const updatedState = {
    ...state.savedSearches[state.selectedSearch],
    currentOptions: options,
    searches: searches,
    searchValue: newSearchTerm
  };

  return assoc(state, ['savedSearches', state.selectedSearch], updatedState);
}


function liveSearchReducer(actionTypes, searchTerms) {
  const terms = searchTerms.map(st => new SearchTerm(st));

  const initialState = {
    potentialOptions: terms,
    selectedSearch: 'All',
    savedSearches: {
      'All': {
        ...emptyState,
        currentOptions: terms
      }
    }
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
  const actionTypes = {
    DELETE_SEARCH_FILTER: 'DELETE_SEARCH_FILTER',
    GO_BACK: 'GO_BACK',
    SUBMIT_FILTER: 'SUBMIT_FILTER'
  };

  return _.mapValues(actionTypes, type => `${namespace.toUpperCase()}_${type}`);
}

function createLiveSearchActions(actionTypes) {
  return {
    deleteSearchFilter: createAction(actionTypes.DELETE_SEARCH_FILTER, (idx) => ({idx})),
    goBack: createAction(actionTypes.GO_BACK),
    submitFilter: createAction(actionTypes.SUBMIT_FILTER, (searchTerm) => ({searchTerm})),
  };
}

export default function makeLiveSearch(namespace, searchOptions) {
  const actionTypes = createLiveSearchActionTypes(namespace);
  const actions = createLiveSearchActions(actionTypes);
  const reducer = liveSearchReducer(actionTypes, searchOptions);
  return { reducer: reducer, actions: actions };
}
