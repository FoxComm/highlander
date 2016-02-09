import _ from 'lodash';
import { createReducer } from 'redux-act';
import { post } from '../lib/search';
import { update } from 'sprout-data';
import { toQuery } from '../elastic/common';
import SearchTerm from '../paragons/search-term';
import makePagination, {makeFetchAction} from './pagination/base';
import reduceReducers from 'reduce-reducers';

const emptyState = {
  isDirty: false,
  isNew: false,
  result: void 0,
  filters: [],
  phrase: '',
};

export default function makeQuickSearch(namespace, searchUrl, searchFilters, phrase) {
  const url = searchUrl;
  const filters = searchFilters.map(st => new SearchTerm(st));
  const initialState = {
    ...emptyState,
    filters,
    phrase
  };

  const fetcher = (phrase, queryFilters = filters) => {
    const esQuery = toQuery(queryFilters, {phrase});
    return post(url, esQuery);
  };

  const {reducer, ...actions} = makePagination(namespace, fetcher, state => _.get(state, `${namespace}.result`));


  const qsReducer = createReducer({
    [actions.searchStart]: (state, [phrase, filters]) => {
      return {
        ...state,
        phrase,
        filters
      };
    },
  }, initialState);

  const reduceInResult = (state, action) => {
    return update(state,
      'result', reducer, action
    );
  };

  return {
    reducer: reduceReducers(qsReducer, reduceInResult),
    actions,
  };
}

