/**
 * @flow
 */

import Api from 'lib/api';
import { dissoc, assoc, update, merge } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import _ from 'lodash';

import { createEmptyProductVariant, configureProductVariant } from 'paragons/product-variant';

export type ProductVariant = {
  id: number,
  feCode?: string,
  attributes: Attributes,
  context: {
    attributes?: Object,
    name: string,
  },
  middlewarehouseSkuId: number,
};

export type NewProductVariant = {
  code?: string,
  feCode?: string,
  attributes: Attributes,
  id: any,
  context: {
    attributes?: Object,
    name: string,
  },
}

const defaultContext = 'default';

export const productVariantNew = createAction('PRODUCT_VARIANT_NEW');
const productVariantClear = createAction('PRODUCT_VARIANT_CLEAR');
export const syncProductVariant = createAction('PRODUCT_VARIANT_SYNC');

const _archiveProductVariant = createAsyncActions(
  'archiveProductVariant',
  (id, context = defaultContext) => {
    return Api.delete(`/product-variants/${context}/${id}`);
  }
);

export const archiveProductVariant = _archiveProductVariant.perform;
export const clearArchiveErrors = _archiveProductVariant.clearErrors;

const _fetchProductVariant = createAsyncActions(
  'fetchProductVariant',
  (id: number, context: string = defaultContext) => {
    return Api.get(`/product-variants/${context}/${id}`);
  }
);

const _createProductVariant = createAsyncActions(
  'createProductVariant',
  (variant: ProductVariant, context: string = defaultContext) => {
    return Api.post(`/product-variants/${context}`, variant);
  }
);

const _updateProductVariant = createAsyncActions(
  'updateProductVariant',
  (variant: ProductVariant, context: string = defaultContext) => {
    const { id } = variant;
    return Api.patch(`/product-variants/${context}/${id}`, variant);
  }
);

export const fetchProductVariant = _fetchProductVariant.perform;
export const createProductVariant = _createProductVariant.perform;
export const updateProductVariant = _updateProductVariant.perform;

export function clearSubmitErrors() {
  return (dispatch: Function) => {
    dispatch(_createProductVariant.clearErrors());
    dispatch(_updateProductVariant.clearErrors());
  };
}

export const clearFetchErrors = _fetchProductVariant.clearErrors;

export function reset() {
  return (dispatch: Function) => {
    dispatch(productVariantClear());
    dispatch(clearSubmitErrors());
    dispatch(clearFetchErrors());
  };
}

export type ProductVariantState = {
  productVariant: ?ProductVariant,
}

const initialState: ProductVariantState = {
  productVariant: null,
};

function updateProductVariantInState(state: ProductVariantState, productVariant: ProductVariant) {
  return {
    ...state,
    productVariant: configureProductVariant(productVariant),
  };
}

const reducer = createReducer({
  [productVariantNew]: (state) => {
    return {
      ...state,
      productVariant: createEmptyProductVariant(),
    };
  },
  [productVariantClear]: state => {
    return dissoc(state, 'productVariant');
  },
  [syncProductVariant]: (state, data) => {
    return update(state, 'productVariant', merge, data);
  },
  [_createProductVariant.succeeded]: updateProductVariantInState,
  [_updateProductVariant.succeeded]: updateProductVariantInState,
  [_fetchProductVariant.succeeded]: updateProductVariantInState,
}, initialState);

export default reducer;
