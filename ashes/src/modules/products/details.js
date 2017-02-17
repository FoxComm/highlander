/**
 * @flow
 */

// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import Api from 'lib/api';
import { createEmptyProduct, configureProduct, duplicateProduct } from 'paragons/product';
import { createAsyncActions } from '@foxcomm/wings';
import { dissoc, assoc, update, merge } from 'sprout-data';

// types
import type { Product } from 'paragons/product';

export type ProductDetailsState = {
  product: ?Product,
  skuVariantMap: Object,
};

const defaultContext = 'default';

export const productNew = createAction('PRODUCTS_NEW');
export const productDuplicate = createAction('PRODUCT_DUPLICATE');
const clearProduct = createAction('PRODUCT_CLEAR');
// synchronizes product in case if we have actions which immediately changes product
// without our request for saving
export const syncProduct = createAction('PRODUCT_SYNC');

const _archiveProduct = createAsyncActions(
  'archiveProduct',
  (id, context = defaultContext) => {
    return Api.delete(`/products/${context}/${id}`);
  }
);
export const archiveProduct = _archiveProduct.perform;
export const clearArchiveErrors = _archiveProduct.clearErrors;

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
  const skus = _.reduce(product.skus, (acc, sku) => {
    const code = _.get(sku, 'attributes.code.v');
    if (sku.feCode) {
      feCodes[sku.feCode] = code || '';
    }
    if (code) {
      return [...acc, dissoc(sku, 'feCode')];
    }
    return acc;
  }, []);

  const variants = _.cloneDeep(product.variants);

  // Wow, this is super-duper ugly.
  for (let i = 0; i < variants.length; i++) {
    const variant = variants[i];
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

  return assoc(product,
    'skus', skus,
    'variants', variants
  );
}

const _updateProduct = createAsyncActions(
  'updateProduct',
  (product: Product, context: string = defaultContext) => {
    if (!product.id) {
      throw new Error('product has no id');
    }

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
  [productNew]: () => ({
    ...initialState,
    product: createEmptyProduct(),
  }),
  [productDuplicate]: (state: ProductDetailsState) => ({
    ...initialState,
    product: duplicateProduct(_.get(state, 'product', {})),
  }),
  [clearProduct]: (state: ProductDetailsState) => dissoc(state, 'product'),
  [syncProduct]: (state: ProductDetailsState, data) => update(state, 'product', merge, data),
  [_fetchProduct.succeeded]: updateProductInState,
  [_updateProduct.succeeded]: updateProductInState,
  [_createProduct.succeeded]: updateProductInState,
}, initialState);


export default reducer;
