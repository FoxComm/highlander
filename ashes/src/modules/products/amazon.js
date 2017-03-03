/**
 * @flow
 */

import { createAction, createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import Api from 'lib/api';

const initialState = {
  suggest: {
    primary: null,
    secondary: [],
  },
};

const _fetchSuggest = createAsyncActions(
  'fetchSuggest',
  (product_id: string, text: string) => {
    return Api.get(`/amazon/categories/suggester?product_id=${product_id}&q=${text}`);
  }
);

// export function fetchSuggest(product_id: string, text: string) {
//   return dispatch => {
//     return dispatch(_fetchSuggest.perform(product_id, text));
//   };
// }

export const fetchSuggest = _fetchSuggest.perform;

const _fetchAmazonCategory = createAsyncActions(
  'fetchAmazonCategory',
  (product_id: string, text: string) => {
    // return Api.get(`/products/${context}/${id}`);
    return new Promise(function(resolve, reject) {
      setTimeout(() => resolve(1), 1000);
    });
  }
);

export function fetchAmazonCategory(product_id: string, text: string) {
  return (dispatch: Function) => {
    return dispatch(_fetchSuggest.perform(product_id, text));
  };
}

const reducer = createReducer({
  [_fetchSuggest.succeeded]: (state, res) => ({ ...state, suggest: res }),
  [_fetchAmazonCategory.started]: (state) => ({ ...state, fields: [] }),
  [_fetchAmazonCategory.succeeded]: (state, res) => ({ ...state, fields: res }),
}, initialState);

export default reducer;
