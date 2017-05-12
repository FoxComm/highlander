/* @flow */

import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';

const _fetchReviews = createAsyncActions(
  'fetchReviews',
  function() {
    return this.api.orders.get('BR11010'); // placeholder api call until we have list reviews endpoint
  }
);

export const fetchReviews = _fetchReviews.perform;

const initialState = {
  reviews: null,
};

const reducer = createReducer({
  [_fetchReviews.succeeded]: (state, response) => {
    return {
      ...state,
      list: response.result.lineItems.skus,
    };
  },
}, initialState);

export default reducer;
