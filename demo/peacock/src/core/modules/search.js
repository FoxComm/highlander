/* @flow */

import { get } from 'lodash';
import { createReducer, createAction } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import { addMatchQuery, defaultSearch } from 'lib/elastic';
import type { Product } from './products';

export type Search = {
  term: string;
  results: Array<Product>;
}

const INITIAL_STATE: Search = {
  term: '',
  isActive: false,
  results: [],
};

const MAX_RESULTS = 1000;
const context = process.env.FIREBIRD_CONTEXT || 'default';

const _search = createAsyncActions('search',
  function searchApiCall(term: string) {
    const payload = addMatchQuery(defaultSearch(context), term);
    return this.api.post(`/search/public/products_catalog_view/_search?size=${MAX_RESULTS}`, payload);
  },
  (payload, term) => [payload, term]
);

export function searchProducts(searchTerm: string) {
  return (dispatch: Function, getState: Function) => {
    const { term, force } = getState().search;
    const { search = {} } = getState().asyncActions;

    if (force || term != searchTerm) {
      dispatch(_search.perform(searchTerm));
    } else if (search.isReady) {
      // we should reset ready flag anyway because createAsyncActions skips first request
      // on client side after state has been hydrated on server
      dispatch(_search.resetReadyFlag());
    }
  };
}

/**
 * External actions
 */
export const toggleActive = createAction('TOGGLE_ACTIVE');
export const forceSearch = createAction('FORCE_SEARCH');

const reducer = createReducer({
  [_search.started]: (state) => {
    return {
      ...state,
      results: [],
      force: false,
    };
  },
  [forceSearch]: state => {
    return {
      ...state,
      force: true,
    };
  },
  [_search.succeeded]: (state, [payload, term]) => {
    return {
      ...state,
      results: payload,
      term,
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
