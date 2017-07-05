// @flow
// if you want suggest skus in your UI use this module

import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import Agni from 'lib/agni';
import * as dsl from 'elastic/dsl';
import { createAsyncActions } from '@foxcomm/wings';

const resetSuggestedSkus = createAction('PRODUCTS_RESET_SUGGESTED_SKUS');

export type SuggestOptions = {
  context?: string,
  useTitle?: boolean,
  omitArchived?: boolean,
}

const _suggestSkus = createAsyncActions(
  'skus-suggest',
  (value: string, options: SuggestOptions = {}) => {
    let filters = _.compact([
      options.context ? dsl.termFilter('context', options.context) : void 0,
      options.omitArchived ? dsl.existsFilter('archivedAt', 'missing') : void 0,
    ]);
    let titleMatch = [];
    if (options.useTitle) {
      titleMatch = [dsl.matchQuery('title', {
        query: value,
        operator: 'and',
      })];
    }

    return Agni.search('sku_search_view', dsl.query({
      bool: {
        filter: filters,
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

export function suggestSkus(value: string, options: SuggestOptions = {}): ActionDispatch {
  return (dispatch: Function) => {
    if (!value) {
      return dispatch(resetSuggestedSkus());
    }

    return dispatch(_suggestSkus.perform(value, options));
  };
}

const initialState = {
  skus: [],
};

const reducer = createReducer({
  [_suggestSkus.succeeded]: (state, response) => {
    return {
      ...state,
      skus: _.get(response, 'result', []),
    };
  },
  [resetSuggestedSkus]: state => {
    return {
      ...state,
      skus: [],
    };
  },
}, initialState);

export default reducer;
