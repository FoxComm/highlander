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

const _searchProducts = createAsyncActions('search',
  function searchApiCall(term: string) {
    const payload = addMatchQuery(defaultSearch(context), term);
    return this.api.post(`/search/public/products_catalog_view/_search?size=${MAX_RESULTS}`, payload);
  }
);

export const searchProducts = _searchProducts.perform;

/**
 * External actions
 */
export const setTerm = createAction('SET_TERM');
export const resetTerm = createAction('RESET_TERM');
export const toggleActive = createAction('TOGGLE_ACTIVE');

const reducer = createReducer({
  [_searchProducts.started]: (state) => {
    return {
      ...state,
      results: [],
    };
  },
  [_searchProducts.succeeded]: (state, payload) => {
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
