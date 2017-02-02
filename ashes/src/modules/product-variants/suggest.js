// @flow
// if you want suggest skus in your UI use this module

import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { post } from 'lib/search';
import * as dsl from 'elastic/dsl';
import { createAsyncActions } from '@foxcomm/wings';

const resetSuggestedVariants = createAction('PRODUCTS_RESET_SUGGESTED_VARIANTS');

export type SuggestOptions = {
  context?: string,
  useTitle?: boolean,
}

const _suggestVariants = createAsyncActions(
  'suggestVariants',
  (value: string, options: SuggestOptions = {}) => {
    const contextFilter = options.context ? [dsl.termFilter('context', options.context)] : void 0;
    let titleMatch = [];
    if (options.useTitle) {
      titleMatch = [dsl.matchQuery('title', {
        query: value,
        operator: 'and',
      })];
    }

    return post('product_variants_search_view/_search', dsl.query({
      bool: {
        filter: contextFilter,
        should: [
          dsl.matchQuery('skuCode', {
            query: value,
            operator: 'and',
          }),
          ...titleMatch
        ],
        minimum_should_match: 1
      },
    }));
  }
);

export function suggestVariants(value: string, options: SuggestOptions = {}): ActionDispatch {
  return (dispatch: Function) => {
    if (!value) {
      return dispatch(resetSuggestedVariants());
    }

    return dispatch(_suggestVariants.perform(value, options));
  };
}

const initialState = {
  variants: [],
};

const reducer = createReducer({
  [_suggestVariants.succeeded]: (state, response) => {
    return {
      ...state,
      variants: _.get(response, 'result', []),
    };
  },
  [resetSuggestedVariants]: state => {
    return {
      ...state,
      variants: [],
    };
  },
}, initialState);

export default reducer;
