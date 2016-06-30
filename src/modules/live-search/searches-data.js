import _ from 'lodash';
import { post } from '../../lib/search';
import { createReducer } from 'redux-act';
import { createNsAction } from './../utils';
import { toQuery, addNativeFilters } from '../../elastic/common';

import makePagination, { makeFetchAction, makeRefreshAction } from '../pagination/base';
import { addPaginationParams } from '../pagination';

// module is responsible for data in search tab

export default function makeDataInSearches(namespace, esUrl, options = {}) {
  const { extraFilters = null, processQuery = _.identity, initialState = {}, rawSorts = [] } = options;

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

  /** Suppress searchSuccess/searchFailure action if performed search not active(e.g., another search selected) */
  const skipProcessingFetch = (getState, idx) => _.get(getState(), [...ns, 'selectedSearch'], null) !== idx;

  const getSelectedSearch = state => {
    const selectedSearch = _.get(state, [...ns, 'selectedSearch']);
    const resultPath = [...ns, 'savedSearches', selectedSearch];
    return _.get(state, resultPath);
  };

  const { reducer, ...actions } = makePagination(namespace, null, null, initialState);

  function fetcher() {
    const { searchState, getState } = this;

    const fetchingSearchIdx = _.get(getState(), [...ns, 'selectedSearch']);
    const selectedSearchState = getSelectedSearch(getState());
    const searchTerms = _.get(selectedSearchState, 'query', []);
    const extraFilters = _.get(getState(), [...ns, 'extraFilters'], extraFilters);
    const jsonQuery = toQuery(searchTerms, {
      sortBy: searchState.sortBy,
      sortRaw: rawSorts.indexOf(_.trim(searchState.sortBy, '-')) != -1
    });

    if (extraFilters) {
      addNativeFilters(jsonQuery, extraFilters);
    }

    const promise = post(addPaginationParams(esUrl, searchState), processQuery(jsonQuery, { searchState, getState }))
      .then(response => {
        if (skipProcessingFetch(getState, fetchingSearchIdx)) {
          promise.abort();
        }

        return response;
      });

    return promise;
  }

  const fetch = makeFetchAction(fetcher, actions, state => getSelectedSearch(state).results);
  const refresh = makeRefreshAction(fetcher, actions, state => getSelectedSearch(state).results);

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
      refresh,
      updateStateAndFetch,
      setExtraFilters,
    }
  };
}
