/**
 * @flow
 */
import Api from '../../lib/api';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { pushState } from 'redux-router';
import {
  configureProduct,
  createEmptyProduct,
  setProductAttribute,
} from '../../paragons/product';

import _ from 'lodash';

export type Error = {
  status: ?number,
  statusText: ?string,
  messages: Array<string>,
};

export type FullProduct = {
  id: ?number,
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
  id: ?number,
  createdAt: ?string,
  attributes: Attributes,
};

export type Attribute = {
  type: string,
  [key:string]: any;
};

export type Attributes = { [key:string]: any };

export type ShadowAttributes = {
  [key:string]: { type: string, ref: string};
};

export type SkuForm = {
  code: ?string,
  attributes: Attributes,
  createdAt: ?string,
};

export type ProductShadow = {
  id: ?number,
  productId: ?number,
  attributes: ShadowAttributes,
  createdAt: ?string,
};

export type SkuShadow = {
  code: ?string,
  attributes: Attributes,
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
export const productNew = createAction('PRODUCTS_NEW');

const setError = createAction('PRODUCTS_SET_ERROR');

export function fetchProduct(id: string, context: string = defaultContext): ActionDispatch {
  return dispatch => {
    if (id.toLowerCase() == 'new') {
      dispatch(productNew());
    } else {
      dispatch(productRequestStart());
      return Api.get(`/products/full/${context}/${id}`)
        .then(
          (product: FullProduct) => dispatch(productRequestSuccess(product)),
          (err: Object) => {
            dispatch(productRequestFailure());
            dispatch(setError(err));
          }
        );
    }
  };
}

export function createProduct(product: FullProduct, context: string = defaultContext): ActionDispatch {
  return dispatch => {
    dispatch(productUpdateStart());
    return Api.post(`/products/full/${context}`, product)
      .then(
        (product: FullProduct) => {
          dispatch(productUpdateSuccess(product));
          dispatch(pushState(null, `/products/${product.form.product.id}`, ''));
        },
        (err: Object) => {
          dispatch(productUpdateFailure());
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
      product: configureProduct(response),
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
        product: setProductAttribute(state.product, label, type, ''),
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
