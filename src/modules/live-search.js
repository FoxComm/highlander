import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import SearchTerm from '../paragons/search-term';
import util from 'util';

/**
 * Deletes a saved search by it's index.
 * @param {object} state The state of the module before deleting the search.
 * @param {int} idx The index of the search to delete.
 * @return {object} The state of the module after deleting the search.
 */
function deleteSearchFilter(state, idx) {
  if (!_.isEmpty(state.searches) && _.isEmpty(state.searchValue)) {
    return {
      ...state,
      searches: _.without(state.searches, state.searches[idx])
    };
  }

  return state;
}

/**
 * Step back through the history of a search term by stripping off the newest
 * level (separated by ':'). If this search term is empty, this is a no-op.
 * @param {object} state The state of the module before going back.
 * @return {object} The updated state of the module after going back.
 */
function goBack(state) {
  const lastColonIdx = _.trim(state.searchValue, ' :').lastIndexOf(':');

  let newSearchTerm = null;
  if (lastColonIdx > 0) {
    newSearchTerm = `${state.searchValue.slice(0, lastColonIdx - 1)} : `;
  } else {
    newSearchTerm = '';
  }

  return updateSearchTerm(state, newSearchTerm);
}

/**
 * Attempts to submit a selected filter. If valid, it will either show the
 * filter's sub-options or it will save the filter as a search.
 * @param {object} state The state of the module before executing the action.
 * @param {string} searchTerm The term that is being submitted.
 * @return {object} The state of the module after the action.
 */
function submitFilter(state, searchTerm) {
  // First update the available terms.
  let options = [];
  let searches = state.searches;
  let newSearchTerm = searchTerm;

  _.forEach(state.potentialOptions, opts => {
    const visibleOptions =  opts.applicableTerms(searchTerm);
    if (!_.isEmpty(visibleOptions)) {
      options = options.concat(visibleOptions);
    }
  });

  // Second, if there is only one term, see if we can turn it into a saved search.
  if (options.length == 1 && options[0].selectTerm(searchTerm)) {
    _.forEach(state.potentialOptions, opts => {
      const visibleOptions = opts.applicableTerms('');
      if (!_.isEmpty(visibleOptions)) {
        options = options.concat(visibleOptions);
      }
    });

    searches = state.searches.concat(searchTerm);
    newSearchTerm = '';
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
