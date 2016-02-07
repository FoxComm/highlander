import _ from 'lodash';
import { post } from '../../lib/search';
import { createReducer } from 'redux-act';
import { assoc } from 'sprout-data';
import { createNsAction } from './utils';
import { toQuery, addNativeFilters } from '../../elastic/common';
import reduceReducers from 'reduce-reducers';

import makePagination, { makeFetchAction } from '../pagination/base';

// module is responsible for data in search tab

export default function makeDataInSearches(namespace, esUrl, options = {}) {
  const { extraFilters = null, processQuery = _.identity } = options;

  const setExtraFilters = createNsAction(namespace, 'SET_EXTRA_FILTERS');
  const ns = namespace.split(/\./);

  const reducer = createReducer({
    [setExtraFilters]: (state, extraFilters) => {
      return assoc(state,
        [...ns, 'extraFilters'], extraFilters
      );
    },
  });

  const getSelectedSearch = state => {
    const selectedSearch = _.get(state, [...ns, 'selectedSearch']);
    const resultPath = [...ns, 'savedSearches', selectedSearch];
    return _.get(state, resultPath);
  };

  const {reducer: paginationReducer, ...actions} = makePagination(namespace);

  const fetcher = ({searchState, getState}) => {
    const searchTerms = _.get(getSelectedSearch(getState()), 'query', []);
    const extraFilters = _.get(getState(), [...ns, 'extraFilters'], extraFilters);
    const jsonQuery = toQuery(searchTerms, {
      sortBy: searchState.sortBy
    });

    if (extraFilters) {
      addNativeFilters(jsonQuery, extraFilters);
    }

    return post(esUrl, processQuery(jsonQuery, {searchState, getState}));
  };

  const fetch = makeFetchAction(fetcher, actions, state => getSelectedSearch(state).results);

  // for overriding updateStateAndFetch in pagination actions
  const updateStateAndFetch = (newState, ...args) => {
    return dispatch => {
      dispatch(actions.updateState(newState));
      dispatch(fetch(...args));
    };
  };

  return {
    reducer: reduceReducers(paginationReducer, reducer),
    actions: {
      ...actions,
      fetch,
      updateStateAndFetch,
      setExtraFilters,
    }
  };
}
