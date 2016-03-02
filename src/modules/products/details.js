/**
 * @flow
 */
import Api from '../../lib/api';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import _ from 'lodash';

export type Context = {
  name: string,
  attributes: { [key:string]: string },
};

export type Error = {
  status: number,
  statusText: string,
  messages: Array<string>,
};

export type Product = {
  id: number,
  context: Context,
  attributes: { [key:string]: any },
  variants: { [key:string]: string },
};

export type ProductDetailsState = {
  err: ?Error,
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
    const error: Error = {
      status: _.get(err, 'response.status'),
      statusText: _.get(err, 'response.statusText', ''),
      messages: _.get(err, 'responseJson.error', []),
    };

    return {
      ...state,
      err: error,
    };
  },
}, initialState);

export default reducer;
