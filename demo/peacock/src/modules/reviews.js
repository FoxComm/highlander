/* @flow */

// libs
import { createReducer, createAction } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';

// actions - private
const _fetchReviewsForUser = createAsyncActions(
  'fetchReviews',
  function(userId: number) {
    const body = {
      query: {
        bool: {
          filter: [
            {term: {userId}},
          ],
        },
      },
    };
    return this.api.reviews.search(body, 5); // placeholder api call until we have list reviews endpoint
  }
);

const _fetchReviewsForSku = createAsyncActions(
  'fetchReviewsForSku',
  function(skuCodes: Array<string>, size: number, from: number) {
    const body = {
      query: {
        bool: {
          filter: [{
            terms: {
              sku: skuCodes,
            },
          }],
        },
      },
    };

    return this.api.reviews.search(body, size, from);
  }
);

const _updateReview = createAsyncActions(
  'updateReview',
  function(reviewId: Number, payload: Object) {
    return this.api.reviews.update(reviewId, payload);
  }
);

// actions - public
export const fetchReviewsForUser = _fetchReviewsForUser.perform;
export const fetchReviewsForSku = _fetchReviewsForSku.perform;
export const updateReview = _updateReview.perform;
export const clearReviews = createAction('REVIEWS_CLEAR');

// redux
const initialState = {
  current: null,
  list: [],
  paginationTotal: 0,
};

const reducer = createReducer({
  [_fetchReviewsForUser.succeeded]: (state, response) => {
    return {
      ...state,
      list: response.result,
    };
  },
  [_fetchReviewsForSku.succeeded]: (state, response) => {
    const currentList = state.list;
    const mergedList = (Object.getOwnPropertyNames(response.result).length === 0)
      ? currentList
      : currentList.concat(response.result);

    return {
      ...state,
      list: mergedList,
      paginationTotal: response.pagination.total,
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
      paginationTotal: initialState.paginationTotal,
    };
  },
}, initialState);

export default reducer;
