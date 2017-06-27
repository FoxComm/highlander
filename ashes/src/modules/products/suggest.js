import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { post } from 'lib/search';
import * as dsl from 'elastic/dsl';
import { createAsyncActions } from '@foxcomm/wings';

const _productSuggest = createAsyncActions(
  'productSuggest',
  (value) => {
    return post('products_search_view/_search?size=100', dsl.query({
      bool: {
        should: [
          dsl.matchQuery('title', {
            query: value,
            operator: 'and',
          })
        ],
        minimum_should_match: 1
      },
    }));
  }
);

const resetSuggestedProducts = createAction('PRODUCTS_RESET_SUGGESTED_PRODUCTS');

export function suggestItems(value) {
  return (dispatch) => {
    return dispatch(_productSuggest.perform(value));
  };
}

const initialState = {
  products: [],
};

const reducer = createReducer({
  [_productSuggest.succeeded]: (state, response) => {
    return {
      products: _.get(response, 'result', []),
    };
  },
  [resetSuggestedProducts]: state => {
    return {
      products: [],
    };
  },
}, initialState);

export default reducer;
