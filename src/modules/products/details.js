/**
 * @flow
 */
import Api from '../../lib/api';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

export type Context = {
  name: string,
  attributes: { [key:string]: string },
};

export type Product = {
  id: number,
  context: Context,
  attributes: { [key:string]: any },
  variants: { [key:string]: string },
};

export type ProductDetailsState = {
  err: ?Object,
  isFetching: boolean,
  product: ?Product,
};

const defaultContext = 'default';

const productRequestStart = createAction('PRODUCTS_REQUEST_START');
const productRequestSuccess = createAction('PRODUCTS_REQUEST_SUCCESS');
const productRequestFailure = createAction('PRODUCTS_REQUEST_FAILURE');

const setError = createAction('PRODUCTS_SET_ERROR');

export function fetchProduct(id: number, context: string = defaultContext): ActionDispatch {
  return dispatch => {
    dispatch(productRequestStart());
    return Api.get(`/products/illuminated/${context}/${id}`)
      .then(
        (product: Product) => dispatch(productRequestSuccess(product)),
        (err: Object) => {
          dispatch(productRequestFailure());
          dispatch(setError(err));
        }
      );
  };
}

const initialState: ProductDetailsState = {
  err: null,
  isFetching: false,
  product: null,
};

const reducer = createReducer({
  [productRequestStart]: (state: ProductDetailsState) => {
    return {
      ...state,
      err: null,
      isFetching: true,
    };
  },
  [productRequestSuccess]: (state: ProductDetailsState, response: Product) => {
    return {
      ...state,
      err: null,
      isFetching: false,
      product: response,
    };
  },
  [productRequestFailure]: (state: ProductDetailsState) => {
    return {
      ...state,
      isFetching: false,
    };
  },
  [setError]: (state: ProductDetailsState, err: Object) => {
    return {
      ...state,
      err: err,
    };
  },
}, initialState);

export default reducer;
