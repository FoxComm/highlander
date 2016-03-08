/**
 * @flow
 */
import Api from '../../lib/api';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { addProductAttribute, configureProduct } from '../../paragons/product';
import _ from 'lodash';

export type Error = {
  status: ?number,
  statusText: ?string,
  messages: Array<string>,
};

export type FullProduct = {
  id: number,
  form: { 
    product: ProductForm,
    skus: Array<SkuForm>,
  },
  shadow: {
    product: ProductShadow,
    skus: Array<SkuShadow>,
  },
};

export type ProductForm = {
  id: number,
  createdAt: string,
  attributes: Attributes,
  variants: { [key:string]: Object },
};

export type Attribute = {
  type: string,
  [key:string]: any,
};

export type Attributes = { [key:string]: Attribute };

export type ShadowAttributes = { [key:string]: string };

export type SkuForm = {
  code: string,
  isActive: boolean,
  attributes: Attributes,
};

export type ProductShadow = {
  id: number,
  productId: number,
  attributes: ShadowAttributes,
  variants: string,
  createdAt: string,
  activeFrom: string,
  activeTo: string,
};

export type SkuShadow = {
  code: string,
  attributes: ShadowAttributes,
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

export const productAddAttribute = createAction('PRODUCTS_ADD_ATTRIBUTE', (label, type) => [label, type]);

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
    const productId = product.form.product.id;
    return Api.patch(`/products/full/${context}/${productId}`, product)
      .then(
        (product: FullProduct) => dispatch(productUpdateSuccess(product)),
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
  [productAddAttribute]: (state: ProductDetailsState, [label, type]) => {
    if (state.product) {
      return {
        ...state,
        product: addProductAttribute(state.product, label, type, defaultContext),
      };
    } else {
      return state;
    }
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
