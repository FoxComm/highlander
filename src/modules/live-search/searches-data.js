import _ from 'lodash';
import { post } from '../../lib/search';
import { createReducer } from 'redux-act';
import { createNsAction } from './../utils';
import { toQuery, addNativeFilters } from '../../elastic/common';

import makePagination, { makeFetchAction } from '../pagination/base';
import { addPaginationParams } from '../pagination';

// module is responsible for data in search tab

export default function makeDataInSearches(namespace, esUrl, options = {}) {
  const { extraFilters = null, processQuery = _.identity, initialState = {} } = options;

  const setExtraFilters = createNsAction(namespace, 'SET_EXTRA_FILTERS');
  const ns = namespace.split(/\./);

  const rootReducer = createReducer({
    [setExtraFilters]: (state, extraFilters) => {
      return {
        ...state,
        extraFilters,
      };
    },
  });

  const getSelectedSearch = state => {
    const selectedSearch = _.get(state, [...ns, 'selectedSearch']);
    const resultPath = [...ns, 'savedSearches', selectedSearch];
    return _.get(state, resultPath);
  };

  const { reducer, ...actions } = makePagination(namespace, null, null, initialState);

  function fetcher() {
    const { searchState, getState } = this;

    const selectedSearchState = getSelectedSearch(getState());
    const searchTerms = _.get(selectedSearchState, 'query', []);
    const phrase = _.get(selectedSearchState, 'phrase');
    const extraFilters = _.get(getState(), [...ns, 'extraFilters'], extraFilters);
    const jsonQuery = toQuery(searchTerms, {
      sortBy: searchState.sortBy,
      phrase: phrase,
    });

    if (extraFilters) {
      addNativeFilters(jsonQuery, extraFilters);
    }

    return post(addPaginationParams(esUrl, searchState), processQuery(jsonQuery, { searchState, getState }));
  }

  const fetch = makeFetchAction(fetcher, actions, state => getSelectedSearch(state).results);

  // for overriding updateStateAndFetch in pagination actions
  const updateStateAndFetch = (newState, ...args) => {
    return dispatch => {
      dispatch(actions.updateState(newState));
      dispatch(fetch(...args));
    };
  };

  return {
    reducer,
    rootReducer,
    actions: {
      ...actions,
      fetch,
      updateStateAndFetch,
      setExtraFilters,
    }
  };
}
