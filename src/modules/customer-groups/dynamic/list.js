//libs
import _ from 'lodash';
import { createReducer } from 'redux-act';

//helpers
import { post } from '../../../lib/search';
import { createNsAction } from './../../utils';
import { toQuery, addNativeFilters } from '../../../elastic/common';

import makePagination, { makeFetchAction } from '../../pagination/base';
import { addPaginationParams } from '../../pagination';


const getState = state => {
  const selectedSearch = _.get(state, 'customerGroups');
  const resultPath = [...ns, 'savedSearches', selectedSearch];

  return _.get(state, 'customerGroups');
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

  return post(addPaginationParams(esUrl, searchState), processQuery(jsonQuery, {searchState, getState}));
}

const fetch = makeFetchAction(fetcher, actions, state => getSelectedSearch(state).results);

// for overriding updateStateAndFetch in pagination actions
const updateStateAndFetch = (newState, ...args) => {
  return dispatch => {
    dispatch(actions.updateState(newState));
    dispatch(fetch(...args));
  };
};

const {reducer, fetch, addEntity, updateStateAndFetch} = makePagination('/groups', 'customerGroups.list');

export {
  reducer as default,
  fetch,
  addEntity as addGroup,
  updateStateAndFetch
};
