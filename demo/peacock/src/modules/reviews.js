/* @flow */

// libs
import { createReducer, createAction } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';

// actions - private
const _fetchReviews = createAsyncActions(
  'fetchReviews',
  function() {
    const body = {
      query: { match_all: {} },
    };
    return this.api.reviews.search(body, 5); // placeholder api call until we have list reviews endpoint
  }
);

const _fetchReviewsForSku = createAsyncActions(
  'fetchReviewsForSku',
  function(sku: string) {
    const body = {
      query: {
        bool: {
          must: [{
            match: { sku },
          }],
        },
      },
    };

    return this.api.reviews.search(body, 5);
  }
);

const _updateReview = createAsyncActions(
  'updateReview',
  function(reviewId: Number, payload: Object) {
    return this.api.reviews.update(reviewId, payload);
  }
);

// actions - public
export const fetchReviews = _fetchReviews.perform;
export const fetchReviewsForSku = _fetchReviewsForSku.perform;
export const updateReview = _updateReview.perform;
export const clearReviews = createAction('REVIEWS_CLEAR');

// redux
const initialState = {
  current: null,
  list: {},
};

const reducer = createReducer({
  [_fetchReviews.succeeded]: (state, response) => {
    return {
      ...state,
      list: response.result,
    };
  },
  [_fetchReviewsForSku.succeeded]: (state, response) => {
    return {
      ...state,
      list: response.result,
    };
  },
  [_updateReview.succeeded]: (state) => {
    return state;
  },
  [clearReviews]: (state) => {
    return {
      ...state,
      current: initialState.current,
      list: initialState.list,
    };
  },
}, initialState);

export default reducer;
