/**
 * @flow
 */
import Api from '../../lib/api';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { configureProduct } from '../../paragons/product';
import _ from 'lodash';

export type Error = {
  status: ?number,
  statusText: ?string,
  messages: Array<string>,
};

export type FullProduct = {
  id: number,
  form: Form,
  shadow: Shadow,
};

type Form = {
  product: ProductForm,
  skus: Array<SkuForm>,
};

export type ProductForm = {
  id: number,
  createdAt: string,
  isActive: boolean,
  attributes: { [key:string]: ProductAttribute },
  variants: { [key:string]: Object },
};

export type ProductAttribute = {
  type: string,
  [key:string]: any,
};

type SkuForm = {
  code: string,
  isActive: boolean,
  attributes: { [key:string]: Object },
};

type Shadow = {
  product: ProductShadow,
  skus: Array<SkuShadow>,
};

export type ProductShadow = {
  id: number,
  productId: number,
  attributes: { [key:string]: string },
  createdAt: string,
};

type SkuShadow = {
  code: string,
  attributes: { [key:string]: string },
  isActive: boolean,
};

export type ProductDetailsState = {
  err: ?Error,
  isFetching: boolean,
  isUpdating: boolean,
  product: ?FullProduct,
};

const defaultContext = 'default';

const productRequestStart = createAction('PRODUCTS_REQUEST_START');
const productRequestSuccess = createAction('PRODUCTS_REQUEST_SUCCESS');
const productRequestFailure = createAction('PRODUCTS_REQUEST_FAILURE');

const productUpdateStart = createAction('PRODUCTS_UPDATE_START');
const productUpdateSuccess = createAction('PRODUCTS_UPDATE_SUCCESS');
const productUpdateFailure = createAction('PRODUCTS_UPDATE_FAILURE');

const setError = createAction('PRODUCTS_SET_ERROR');

export function fetchProduct(id: number, context: string = defaultContext): ActionDispatch {
  return dispatch => {
    dispatch(productRequestStart());
    return Api.get(`/products/full/${context}/${id}`)
      .then(
        (product: FullProduct) => dispatch(productRequestSuccess(product)),
        (err: Object) => {
          dispatch(productRequestFailure());
          dispatch(setError(err));
        }
      );
  };
}

export function updateProduct(product: FullProduct, context: string = defaultContext): ActionDispatch {
  return dispatch => {
    dispatch(productUpdateStart());
    dispatch(productUpdateSuccess());
  };
}

const initialState: ProductDetailsState = {
  err: null,
  isFetching: false,
  isUpdating: false,
  product: null,
  response: null,
};

const reducer = createReducer({
  [productRequestStart]: (state: ProductDetailsState) => {
    return {
      ...state,
      err: null,
      isFetching: true,
    };
  },
  [productRequestSuccess]: (state: ProductDetailsState, response: FullProduct) => {
    return {
      ...state,
      err: null,
      isFetching: false,
      product: configureProduct(response),
    };
  },
  [productRequestFailure]: (state: ProductDetailsState) => {
    return {
      ...state,
      isFetching: false,
    };
  },
  [productUpdateStart]: (state: ProductDetailsState) => {
    return {
      ...state,
      isUpdating: true,
    };
  },
  [productUpdateSuccess]: (state: ProductDetailsState, response: FullProduct) => {
    return {
      ...state,
      err: null,
      isUpdating: false,
      product: response,
    };
  },
  [productUpdateFailure]: (state: ProductDetailsState) => {
    return {
      ...state,
      isUpdating: false,
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
