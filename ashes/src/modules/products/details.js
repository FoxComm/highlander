/**
 * @flow
 */

// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import Api from 'lib/api';
import { createEmptyProduct, configureProduct } from 'paragons/product';
import createAsyncActions from '../async-utils';
import { dissoc } from 'sprout-data';

// types
import type { Product } from 'paragons/product';

export type ProductDetailsState = {
  product: ?Product,
  skuVariantMap: Object,
};

const defaultContext = 'default';

export const productNew = createAction('PRODUCTS_NEW');
const clearProduct = createAction('PRODUCT_CLEAR');

const _archiveProduct = createAsyncActions(
  'archiveProduct',
  (id, context = defaultContext) => {
    return Api.delete(`/products/${context}/${id}`);
  }
);
export const archiveProduct = _archiveProduct.perform;

export function sanitizeError(error: string): string {
  if (error.indexOf('sku_code violates check constraint "sku_code_check"') != -1) {
    return 'Product must contain a SKU.';
  }

  return error;
}

const _fetchProduct = createAsyncActions(
  'fetchProduct',
  (id: string, context: string = defaultContext) => {
    return Api.get(`/products/${context}/${id}`);
  }
);

export function fetchProduct(id: string, context: string = defaultContext): ActionDispatch {
  return dispatch => {
    if (id.toLowerCase() == 'new') {
      dispatch(productNew());
    } else {
      return dispatch(_fetchProduct.perform(id, context));
    }
  };
}

const _createProduct = createAsyncActions(
  'createProduct',
  (product: Product, context: string = defaultContext) => {
    return Api.post(`/products/${context}`, cleanProductPayload(product));
  }
);

function cleanProductPayload(product) {
  // get rid of temp. skus
  const feCodes = {};
  product.skus = _.reduce(product.skus, (acc, sku) => {
    const code = _.get(sku, 'attributes.code.v');
    if (sku.feCode) {
      feCodes[sku.feCode] = code || '';
    }
    if (code) {
      return [...acc, dissoc(sku, 'feCode')];
    }
    return acc;
  }, []);

  product.variants = _.cloneDeep(product.variants);

  // Wow, this is super-duper ugly.
  for (let i = 0; i < product.variants.length; i++) {
    const variant = product.variants[i];
    for (let j = 0; j < variant.values.length; j++) {
      const value = variant.values[j];
      value.skuCodes = _.reduce(value.skuCodes, (acc, code) => {
        if (code) {
          const value = _.get(feCodes, code, code);
          if (value) {
            return [...acc, value];
          }
        }
        return acc;
      }, []);
    }
  }

  return product;
}

const _updateProduct = createAsyncActions(
  'updateProduct',
  (product: Product, context: string = defaultContext) => {
    return Api.patch(`/products/${context}/${product.id}`, cleanProductPayload(product));
  }
);

export const createProduct = _createProduct.perform;
export const updateProduct = _updateProduct.perform;

function updateProductInState(state: ProductDetailsState, response) {
  const product = configureProduct(response);
  return { ...state, product };
}

const initialState: ProductDetailsState = {
  product: null,
  skuVariantMap: {},
};

export function clearSubmitErrors() {
  return (dispatch: Function) => {
    dispatch(_createProduct.clearErrors());
    dispatch(_updateProduct.clearErrors());
  };
}

export const clearFetchErrors = _fetchProduct.clearErrors;

export function reset() {
  return (dispatch: Function) => {
    dispatch(clearProduct());
    dispatch(clearSubmitErrors());
    dispatch(clearFetchErrors());
  };
}

const reducer = createReducer({
  [productNew]: (state: ProductDetailsState) => {
    return {
      ...initialState,
      product: createEmptyProduct(),
    };
  },
  [clearProduct]: (state: ProductDetailsState) => {
    return dissoc(state, 'product');
  },
  [_fetchProduct.succeeded]: updateProductInState,
  [_updateProduct.succeeded]: updateProductInState,
  [_createProduct.succeeded]: updateProductInState,
}, initialState);


export default reducer;
