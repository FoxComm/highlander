/**
 * @flow
 */
import Api from '../../lib/api';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { push } from 'react-router-redux';
import {
  createEmptyProduct,
} from '../../paragons/product';

import type { SkuForm, SkuShadow } from '../../paragons/sku';

import _ from 'lodash';

export type Error = {
  status: ?number,
  statusText: ?string,
  messages: Array<string>,
};

export type Product = {
  id: ?number,
  attributes: { [key:string]: { t: string, v: any } },
  skus: Array<Object>,
};

export type Attribute = {
  type: string,
  [key:string]: any;
};

export type Attributes = { [key:string]: any };

export type ShadowAttributes = {
  [key:string]: { type: string, ref: string};
};

export type ProductShadow = {
  id: ?number,
  productId: ?number,
  attributes: ShadowAttributes,
  createdAt: ?string,
};

export type Variant = {
  name: ?string,
  type: ?string,
  values: { [key:string]: VariantValue },
};

export type VariantValue = {
  id: number,
  swatch: ?string,
  image: ?string,
};

export type ProductDetailsState = {
  err: ?Error,
  isFetching: boolean,
  isUpdating: boolean,
  product: ?Product,
};

const defaultContext = 'default';

const productRequestStart = createAction('PRODUCTS_REQUEST_START');
const productRequestSuccess = createAction('PRODUCTS_REQUEST_SUCCESS');
const productRequestFailure = createAction('PRODUCTS_REQUEST_FAILURE');

const productUpdateStart = createAction('PRODUCTS_UPDATE_START');
const productUpdateSuccess = createAction('PRODUCTS_UPDATE_SUCCESS');
const productUpdateFailure = createAction('PRODUCTS_UPDATE_FAILURE');

export const productNew = createAction('PRODUCTS_NEW');

const setError = createAction('PRODUCTS_SET_ERROR');

export function fetchProduct(id: string, context: string = defaultContext): ActionDispatch {
  return dispatch => {
    if (id.toLowerCase() == 'new') {
      dispatch(productNew());
    } else {
      dispatch(productRequestStart());
      return Api.get(`/products/${context}/${id}`)
        .then(
          (product: Product) => dispatch(productRequestSuccess(product)),
          (err: Object) => {
            dispatch(productRequestFailure());
            dispatch(setError(err));
          }
        );
    }
  };
}

export function createProduct(product: Product, context: string = defaultContext): ActionDispatch {
  return dispatch => {
    dispatch(productUpdateStart());
    return Api.post(`/products/${context}`, product)
      .then(
        (product: Product) => {
          dispatch(productUpdateSuccess(product));
          dispatch(push(`/products/${context}/${product.id}`));
        },
        (err: Object) => {
          dispatch(productUpdateFailure());
          dispatch(setError(err));
        }
      );
  };
}

export function updateProduct(product: Product, context: string = defaultContext): ActionDispatch {
  return dispatch => {
    dispatch(productUpdateStart());
    return Api.patch(`/products/${context}/${product.id}`, product)
      .then(
        (product: Product) => dispatch(productUpdateSuccess(product)),
        (err: Object) => {
          dispatch(productUpdateFailure());
          dispatch(setError(err));
        }
      );
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
  [productNew]: (state: ProductDetailsState) => {
    return {
      ...initialState,
      product: createEmptyProduct(),
    };
  },
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
  [productUpdateStart]: (state: ProductDetailsState) => {
    return {
      ...state,
      isUpdating: true,
    };
  },
  [productUpdateSuccess]: (state: ProductDetailsState, response: Product) => {
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
