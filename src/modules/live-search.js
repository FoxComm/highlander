import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import SearchTerm from '../paragons/search-term';
import util from 'util';
import { assoc } from 'sprout-data';

const emptyState = {
  isDirty: false,
  isEditingName: false,
  isNew: false,
  options: [],
  searches: [],
  searchValue: '',
  selectedIndex: -1
};

function cloneSearch(state) {
  const toClone = {
    ...state.savedSearches[state.selectedSearch],
    name: '',
    isEditingName: true,
    isNew: true
  };

  const newState = {
    ...state,
    selectedSearch: state.savedSearches.length,
    savedSearches: [...state.savedSearches, toClone]
  };

  return newState;
}

function deleteSearchFilter(state, idx) {
  const curSearches = state.savedSearches[state.selectedSearch].searches;
  const curValue = state.savedSearches[state.selectedSearch].searchValue;

  if (!_.isEmpty(curSearches) && _.isEmpty(curValue)) {
    const updatedState = {
      ...state.savedSearches[state.selectedSearch],
      searches: _.without(curSearches, curSearches[idx])
    };
    return assoc(state, ['savedSearches', state.selectedSearch], updatedState);
  }

  return state;
}

function editSearchNameStart(state, idx) {
  const newState = selectSavedSearch(state, idx);
  return assoc(newState, ['savedSearches', newState.selectedSearch, 'isEditingName'], true);
}

function editSearchNameCancel(state) {
  const currentState = state.savedSearches[state.selectedSearch];
  if (currentState.isNew) {
    const searches = [
      ...state.savedSearches.slice(0, state.selectedSearch),
      ...state.savedSearches.slice(state.selectedSearch + 1)
    ];
    return {
      ...state,
      savedSearches: searches,
      selectedSearch: 0
    };
  }
  return assoc(state, ['savedSearches', state.selectedSearch, 'isEditingName'], false);
}

function editSearchNameComplete(state, newName) {
  if (!_.isEmpty(newName)) {
    const newState = assoc(state,
      ['savedSearches', state.selectedSearch, 'name'], newName,
      ['savedSearches', state.selectedSearch, 'isEditingName'], false
    );
    return newState;
  }

  return state;
}

function goBack(state) {
  const curState = state.savedSearches[state.selectedSearch].searchValue;
  const lastColonIdx = _.trim(curState, ' :').lastIndexOf(':');
  const newSearchTerm = lastColonIdx > 0 ? `${curState.slice(0, lastColonIdx - 1)} : ` : '';
  return submitFilter(state, newSearchTerm);
}

function saveSearch(state) {
  return assoc(state, ['savedSearches', state.selectedSearch, 'isDirty'], false);
}

function selectSavedSearch(state, idx) {
  if (idx > -1 && idx < state.savedSearches.length) {
    return assoc(state,
      ['selectedSearch'], idx,
      ['savedSearches', state.selectedSearch, 'isEditingName'], false
    );
  }

  return state;
}

function submitFilter(state, searchTerm) {
  // First update the available terms.
  let searches = state.savedSearches[state.selectedSearch].searches;
  let newSearchTerm = searchTerm;
  let options = SearchTerm.potentialTerms(state.potentialOptions, searchTerm);
  let isDirty = state.savedSearches[state.selectedSearch].isDirty;

  // Second, if there is only one term, see if we can turn it into a saved search.
  if (options.length == 1 && options[0].selectTerm(searchTerm)) {
    newSearchTerm = '';
    options = SearchTerm.potentialTerms(state.potentialOptions, newSearchTerm);
    searches = [...state.savedSearches[state.selectedSearch].searches, searchTerm];
    isDirty = true;
  }

  // Third, update the state.
  const updatedState = {
    ...state.savedSearches[state.selectedSearch],
    currentOptions: options,
    isDirty: isDirty,
    searches: searches,
    searchValue: newSearchTerm
  };

  return assoc(state, ['savedSearches', state.selectedSearch], updatedState);
}

function liveSearchReducer(actionTypes, searchTerms) {
  const terms = searchTerms.map(st => new SearchTerm(st));

  const initialState = {
    potentialOptions: terms,
    selectedSearch: 0,
    savedSearches: [
      {
        ...emptyState,
        name: 'All',
        currentOptions: terms
      }, {
        ...emptyState,
        name: 'Remorse Hold',
        currentOptions: terms,
        searches: ['Order : State : Remorse Hold']
      }, {
        ...emptyState,
        name: 'Manual Hold',
        currentOptions: terms,
        searches: ['Order : State : Manual Hold']
      }, {
        ...emptyState,
        name: 'Fraud Hold',
        currentOptions: terms,
        searches: ['Order : State : Fraud Hold']
      }
    ]
  };

  return (state = initialState, action) => {
    const payload = action.payload;

    switch (action.type) {
      case actionTypes.CLONE_SEARCH:
        return cloneSearch(state);

      case actionTypes.EDIT_SEARCH_NAME_START:
        return editSearchNameStart(state, payload.idx);

      case actionTypes.EDIT_SEARCH_NAME_CANCEL:
        return editSearchNameCancel(state);

      case actionTypes.EDIT_SEARCH_NAME_COMPLETE:
        return editSearchNameComplete(state, payload.newName);

      case actionTypes.DELETE_SEARCH_FILTER:
        return deleteSearchFilter(state, payload.idx);

      case actionTypes.GO_BACK:
        return goBack(state);

      case actionTypes.SAVE_SEARCH:
        return saveSearch(state);

      case actionTypes.SELECT_SAVED_SEARCH:
        return selectSavedSearch(state, payload.idx);

      case actionTypes.SUBMIT_FILTER:
        return submitFilter(state, payload.searchTerm);

      default:
        return state;
    }
  };
}

function createLiveSearchActionTypes(namespace) {
  const actionTypes = {
    CLONE_SEARCH: 'CLONE_SEARCH',
    DELETE_SEARCH_FILTER: 'DELETE_SEARCH_FILTER',
    EDIT_SEARCH_NAME_START: 'EDIT_SEARCH_START',
    EDIT_SEARCH_NAME_CANCEL: 'EDIT_SEARCH_CANCEL',
    EDIT_SEARCH_NAME_COMPLETE: 'EDIT_SEARCH_NAME_COMPLETE',
    GO_BACK: 'GO_BACK',
    SAVE_SEARCH: 'SAVE_SEARCH',
    SELECT_SAVED_SEARCH: 'SELECT_SAVED_SEARCH',
    SUBMIT_FILTER: 'SUBMIT_FILTER'
  };

  return _.mapValues(actionTypes, type => `${namespace.toUpperCase()}_${type}`);
}

function createLiveSearchActions(actionTypes) {
  return {
    cloneSearch: createAction(actionTypes.CLONE_SEARCH),
    deleteSearchFilter: createAction(actionTypes.DELETE_SEARCH_FILTER, (idx) => ({idx})),
    editSearchNameStart: createAction(actionTypes.EDIT_SEARCH_NAME_START, (idx) => ({idx})),
    editSearchNameCancel: createAction(actionTypes.EDIT_SEARCH_NAME_CANCEL),
    editSearchNameComplete: createAction(actionTypes.EDIT_SEARCH_NAME_COMPLETE, (newName) => ({newName})),
    goBack: createAction(actionTypes.GO_BACK),
    saveSearch: createAction(actionTypes.SAVE_SEARCH),
    selectSavedSearch: createAction(actionTypes.SELECT_SAVED_SEARCH, (idx) => ({idx})),
    submitFilter: createAction(actionTypes.SUBMIT_FILTER, (searchTerm) => ({searchTerm}))
  };
}

export default function makeLiveSearch(namespace, searchOptions) {
  const actionTypes = createLiveSearchActionTypes(namespace);
  const actions = createLiveSearchActions(actionTypes);
  const reducer = liveSearchReducer(actionTypes, searchOptions);
  return { reducer: reducer, actions: actions };
}
