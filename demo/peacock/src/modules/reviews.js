/* @flow */

// libs
import { createReducer, createAction } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import _ from 'lodash';

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
  function(reviewId: number, payload: Object) {
    return this.api.reviews.update(reviewId, payload);
  }
);

const _removeReview = createAsyncActions(
  'removeReview',
  function(reviewId: number) {
    const { dispatch } = this;
    return this.api.reviews.delete(reviewId).then((resp) => {
      dispatch(cleanUpReviews(reviewId));
    });
  }
);

const cleanUpReviews = createAction('CLEAN_UP_REVIEWS');

// actions - public
export const fetchReviewsForUser = _fetchReviewsForUser.perform;
export const fetchReviewsForSku = _fetchReviewsForSku.perform;
export const updateReview = _updateReview.perform;
export const removeReview =  _removeReview.perform;
export const clearReviews = createAction('REVIEWS_CLEAR');
export const toggleReviewsModal = createAction('TOGGLE_REVIEWS_MODAL');

// redux
const initialState = {
  current: null,
  list: [],
  paginationTotal: 0,
  reviewsModalVisible: false,
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
  [toggleReviewsModal]: (state) => {
    const current = _.get(state, 'reviewsModalVisible', false);
    return {
      ...state,
      reviewsModalVisible: !current,
    };
  },
  [cleanUpReviews]: (state, reviewId) => {
    const newList = _.filter(state.list, (review) => review.id !== reviewId);
    console.log('newList -> ', newList);

    return {
      ...state,
      list: newList,
    };
  }
}, initialState);

export default reducer;
