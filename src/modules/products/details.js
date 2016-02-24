/**
 * @flow
 */
import type { ProductResponse } from './sample-products';

import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { getProductResponse } from './sample-products';

type ProductDetailsState = {
  isFetching: boolean,
  product: ?ProductResponse,
};

const productRequestStart = createAction('PRODUCTS_REQUEST_START');
const productRequestSuccess = createAction('PRODUCTS_REQUEST_SUCCESS');

export function fetchProduct(id: number): ActionDispatch {
  return dispatch => {
    dispatch(productRequestStart());
    const res = getProductResponse(id);
    dispatch(productRequestSuccess(res));
  }
}

const initialState: ProductDetailsState = {
  isFetching: false,
  product: null,
};

const reducer = createReducer({
  [productRequestStart]: (state: ProductDetailsState) => {
    return {
      ...state,
      isFetching: true,
    };
  },
  [productRequestSuccess]: (state: ProductDetailsState, response: ProductResponse) => {
    return {
      ...state,
      isFetching: false,
      product: response,
    };
  },
}, initialState);

export default reducer;
