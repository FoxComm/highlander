/* @flow */

import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';

const _fetchReviews = createAsyncActions(
  'fetchReviews',
  function() {
    const body = {
      query: { match_all: {} }
    };
    return this.api.reviews.search(body, 5); // placeholder api call until we have list reviews endpoint
  }
);

export const fetchReviews = _fetchReviews.perform;

const initialState = {
  current: null,
  list: {},
};

const reducer = createReducer({
  [_fetchReviews.succeeded]: (state, response) => {
    return {
      ...state,
      list: response.result
    };
  },
}, initialState);

export default reducer;
