// @flow
// if you want suggest skus in your UI use this module

import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { post } from 'lib/search';
import * as dsl from 'elastic/dsl';
import createAsyncActions from 'modules/async-utils';

const resetSuggestedSkus = createAction('PRODUCTS_RESET_SUGGESTED_SKUS');

const _suggestSkus = createAsyncActions(
  'skus-suggest',
  (code: string, context: ?string) => {
    const contextFilter = context ? [dsl.termFilter('context', context)] : void 0;

    return post('sku_search_view/_search', dsl.query({
      bool: {
        filter: contextFilter,
        must: [
          dsl.matchQuery('skuCode', {
            query: code,
            operator: 'and',
          }),
        ]
      },
    }));
  }
);


export function suggestSkus(code: string, context: ?string = null): ActionDispatch {
  return (dispatch: Function) => {
    if (!code) {
      return dispatch(resetSuggestedSkus());
    }

    return dispatch(_suggestSkus.perform(code, context));
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
