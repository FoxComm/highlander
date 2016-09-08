/* @flow */

import { get } from 'lodash';
import { createReducer, createAction } from 'redux-act';
import createAsyncActions from './async-utils';
import { addMatchQuery, defaultSearch } from 'lib/elastic';
import type { Product } from './products';

export type Search = {
  term: string;
  results: Array<Product>;
}

const INITIAL_STATE:Search = {
  term: '',
  isActive: false,
  results: [],
};

const MAX_RESULTS = 1000;
const context = process.env.FIREBRAND_CONTEXT || 'default';

/**
 * Generate search api call actions and reducer
 * Mocked for now
 */
function searchApiCall(searchString: string): global.Promise {
  const payload = addMatchQuery(defaultSearch(context), searchString);
  return this.api.post(`/search/public/products_catalog_view/_search?size=${MAX_RESULTS}`, payload);
}

const { fetch, ...searchActions } = createAsyncActions('search', searchApiCall);

/**
 * External actions
 */
export const setTerm = createAction('SET_TERM');
export const resetTerm = createAction('RESET_TERM');
export const toggleActive = createAction('TOGGLE_ACTIVE');
export { fetch };

const reducer = createReducer({
  [searchActions.succeeded]: (state, payload) => {
    return {
      ...state,
      results: payload,
    };
  },
  [setTerm]: (state, payload) => {
    return {
      ...state,
      term: payload,
    };
  },
  [resetTerm]: (state) => {
    return {
      ...state,
      term: '',
    };
  },
  [toggleActive]: (state) => {
    return {
      ...state,
      isActive: !get(state, 'isActive', false),
    };
  },
}, INITIAL_STATE);

export default reducer;
