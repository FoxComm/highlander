/**
 * @flow
 */
import Api from '../../lib/api';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import Product from '../../paragons/product';
import _ from 'lodash';

export type Error = {
  status: ?number,
  statusText: ?string,
  messages: Array<string>,
};

export type ProductResponse = {
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
  product: ?Product,
  response: ?ProductResponse,
};

const defaultContext = 'default';

const productRequestStart = createAction('PRODUCTS_REQUEST_START');
const productRequestSuccess = createAction('PRODUCTS_REQUEST_SUCCESS');
const productRequestFailure = createAction('PRODUCTS_REQUEST_FAILURE');

const setError = createAction('PRODUCTS_SET_ERROR');

export function fetchProduct(id: number, context: string = defaultContext): ActionDispatch {
  return dispatch => {
    dispatch(productRequestStart());
    return Api.get(`/products/full/${context}/${id}`)
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
  [productRequestSuccess]: (state: ProductDetailsState, response: ProductResponse) => {
    const productForm = response.form.product;
    const productShadow = response.shadow.product;

    return {
      ...state,
      err: null,
      isFetching: false,
      product: new Product(productForm, productShadow),
      response: response,
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
