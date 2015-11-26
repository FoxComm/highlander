import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';

const initialState = {
  currentOptions: [],
  isVisible: false,
  potentialOptions: [],
  inputValue: '',
  displayValue: '',
  selectedIndex: -1,
  searches: []
};

/**
 * Deletes a saved search by it's index.
 * @param {object} state The state of the module before deleting the search.
 * @param {int} idx The index of the search to delete.
 * @return {object} The state of the module after deleting the search.
 */
function deleteSearchFilter(state, idx) {
  if (!_.isEmpty(state.searches) && _.isEmpty(state.inputValue)) {
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
  const lastColonIdx = _.trim(state.inputValue, ' :').lastIndexOf(':');

  let newSearchTerm = null;
  if (lastColonIdx > 0) {
    newSearchTerm = `${state.inputValue.slice(0, lastColonIdx - 1)} : `;
  } else {
    newSearchTerm = '';
  }

  return updateSearchTerm(state, newSearchTerm);
}

/**
 * Moves the selected item down one increment (increases index) if available.
 * If not, keeps the selection in the same place.
 * @param {object} state The state of the module before changing selection.
 * @return {object} The updated state of the module after changing selection.
 */
function selectDown(state) {
  if (state.selectedIndex + 1 < state.currentOptions.length) {
    const newIndex = state.selectedIndex + 1;
    const currentOption = state.currentOptions[newIndex];
    return {
      ...state,
      isVisible: true,
      displayValue: optionDisplay(currentOption),
      selectedIndex: newIndex
    };
  }

  return state;
}

/**
 * Moves the selected item up one increment (decreases index) if available.
 * If not, keeps the selection in the same place.
 * @param {object} state The state of the module before changing selection.
 * @return {object} The updated state of the module after changing selection.
 */
function selectUp(state) {
  const selectedIndex = state.selectedIndex;
  if (selectedIndex > 0) {
    const currentOption = state.currentOptions[selectedIndex - 1];
    return {
      ...state,
      isVisible: true,
      displayValue: optionDisplay(currentOption),
      selectedIndex: selectedIndex - 1
    };
  } else if (selectedIndex == 0) {
    return {
      ...state,
      selectedIndex: -1,
      displayValue: state.inputValue
    };
  } else {
    return {
      ...state,
      isVisible: false
    };
  }
}

/**
 * Attempts to submit a selected filter. If valid, it will either show the
 * filter's sub-options or it will save the filter as a search.
 * @param {object} state The state of the module before executing the action.
 * @return {object} The state of the module after the action.
 */
function submitFilter(state) {
  const options = state.currentOptions;

  let selectedIndex = null;
  if (options.length == 1) {
    selectedIndex = 0;
  } else {
    selectedIndex = state.selectedIndex;
  }

  if (selectedIndex == -1 || options.length == 0) {
    // TODO: This will implement the search, not the filter.
    return state;
  }

  const option = options[selectedIndex];

  if (option.type == 'value' &&
      _.trim(state.displayValue, ' :').length > option.display.length) {

    // Turn it into a search.
    return {
      ...updateSearchTerm(state, ''),
      searches: Array.concat(state.searches, state.displayValue),
      isVisible: false
    };
  } else {
    // Select it in the list.
    return updateSearchTerm(
      state,
      `${option.display} : `,
      [{...option, term: option.display}]
    );
  }

  return state;
}

/**
 * Updates the current search term. Upon receiving the updated term, the
 * available search options will be recomputed and the decision about whether
 * to set the search options as visible will be made.
 * @param {object} state The state of the module before updating search.
 * @param {string} searchTerm The new term with which to update search.
 * @param {array} options Optional list of potential search options.
 * @return {object} The state of the module after updating search.
 */
function updateSearchTerm(state, searchTerm, options) {
  const potentialOptions = options || state.potentialOptions;
  const currentOptions = visibleOptions(potentialOptions, searchTerm);

  return {
    ...state,
    isVisible: currentOptions.length > 0,
    inputValue: searchTerm,
    displayValue: searchTerm,
    currentOptions: currentOptions,
    potentialOptions: potentialOptions
  };
}

function liveSearchReducer(actionTypes) {
  return (state = initialState, action) => { 
    const payload = action.payload;

    switch (action.type) {
      case actionTypes.DELETE_SEARCH_FILTER:
        return deleteSearchFilter(state, payload.idx);

      case actionTypes.GO_BACK:
        return goBack(state);

      case actionTypes.SELECT_DOWN:
        return selectDown(state);

      case actionTypes.SELECT_UP:
        return selectUp(state);

      case actionTypes.SUBMIT_FILTER:
        return submitFilter(state);

      case actionTypes.UPDATE_SEARCH:
        return updateSearchTerm(state, payload.searchTerm, payload.options);

      default:
        return state;
    }
  };
}

/**
 * Implementation of that algorithm that determines what search options
 * should be shown in this control.
 * @param {array} optionsList The complete set of options that could be shown.
 * @param {string} term The search term to filter by.
 */
function visibleOptions(optionsList, term, prefix = '') {
  if (!_.isEmpty(prefix)) {
    prefix = `${prefix} : `;
  }

  const opts = _.transform(optionsList, (result, option) => {
    const nSearchTerm = term.toLowerCase();
    const optionTerm = `${prefix}${option.term}`;
    const nOptionTerm = optionTerm.toLowerCase();

    if (nSearchTerm <= nOptionTerm) {
      if (_.startsWith(nOptionTerm, nSearchTerm)) {
        result.push({...option, display: optionTerm});
      }
    } else if (_.startsWith(nSearchTerm, nOptionTerm)) {
      if (option.type == 'object') {
        const nestedOptions = visibleOptions(option.options, term, optionTerm);
        _.forEach(nestedOptions, option => {
          result.push({
            ...option,
            display: `${prefix}${option.display}`
          });
        });
      } else if (option.type == 'enum') {
        _.forEach(option.suggestions, suggestion => {
          result.push({
            ...option,
            display: optionTerm,
            action: suggestion,
            suggestions: [],
            type: 'value'
          });
        });
      } else {
        result.push({
          ...option,
          display: optionTerm,
          type: 'value'
        });
      }   
    }
  });

  return opts;
}

function optionDisplay(option) {
  if (!_.isEmpty(option.action)) {
    return `${option.display} : ${option.action}`;
  } else {
    return `${option.display} : `;
  } 
};

function createLiveSearchActionTypes(namespace) {
  const actionTypes = [
    'DELETE_SEARCH_FILTER',
    'GO_BACK',
    'SELECT_DOWN',
    'SELECT_UP',
    'SUBMIT_FILTER',
    'UPDATE_SEARCH'
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
    selectDown: _createAction(namespace, 'SELECT_DOWN'),
    selectUp: _createAction(namespace, 'SELECT_UP'),
    submitFilter: _createAction(namespace, 'SUBMIT_FILTER'),
    updateSearch: _createAction(namespace, 'UPDATE_SEARCH', (searchTerm, options) => ({searchTerm, options}))
  };
}

export default function makeLiveSearch(namespace) {
  const actions = createLiveSearchActions(namespace);
  const actionTypes = createLiveSearchActionTypes(namespace);
  const reducer = liveSearchReducer(actionTypes);
  return { reducer: reducer, actions: actions };
}
