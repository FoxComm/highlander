// @flow

import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { post } from 'lib/search';
import * as dsl from 'elastic/dsl';
import { createAsyncActions } from '@foxcomm/wings';

const resetSuggestedSkus = createAction('RESET_SUGGESTED_SKUS');

const _suggestSkus = createAsyncActions(
  'suggestSkus',
  (value: string) => {
    return post('inventory_search_view/_search', dsl.query({
      bool: {
        should: [
          dsl.matchQuery('sku', {
            query: value,
            type: 'phrase_prefix',
          }),
        ],
        minimum_should_match: 1
      },
    }));
  }
);

export function suggestSkus(value: string): ActionDispatch {
  return (dispatch: Function) => {
    if (!value) {
      return dispatch(resetSuggestedSkus());
    }

    return dispatch(_suggestSkus.perform(value));
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
