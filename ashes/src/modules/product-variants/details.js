/**
 * @flow
 */

import Api from 'lib/api';
import { dissoc, assoc, update, merge } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import _ from 'lodash';

import { createEmptySku } from 'paragons/sku';

import { pushStockItemChanges } from '../inventory/warehouses';

export type ProductVariant = {
  code?: string,
  feCode?: string,
  attributes: Attributes,
  id: any,
  context: {
    attributes?: Object,
    name: string,
  }
};

const defaultContext = 'default';

export const productVariantNew = createAction('PRODUCT_VARIANT_NEW');
const skuClear = createAction('SKU_CLEAR');
export const syncSku = createAction('SKU_SYNC');

const _archiveProductVariant = createAsyncActions(
  'archiveProductVariant',
  (code, context = defaultContext) => {
    return Api.delete(`/product-variants/${context}/${code}`);
  }
);

export const archiveProductVariant = _archiveProductVariant.perform;
export const clearArchiveErrors = _archiveProductVariant.clearErrors;

const _fetchProductVariant = createAsyncActions(
  'fetchProductVariant',
  (code: string, context: string = defaultContext) => {
    return Api.get(`/product-variants/${context}/${code}`);
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
  function(variant: ProductVariant, context: string = defaultContext) {
    const { dispatch, getState } = this;
    const oldVariant = _.get(getState(), ['productVariants', 'details', 'productVariant', 'attributes', 'code', 'v']);
    if (oldVariant) {
      const stockItemsPromise = dispatch(pushStockItemChanges(oldVariant));
      const updatePromise = Api.patch(`/product-variants/${context}/${oldVariant}`, variant);
      return Promise.all([updatePromise, stockItemsPromise]).then(([updateResponse]) => {
        return updateResponse;
      });
    }
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
    dispatch(skuClear());
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
    productVariant,
  };
}

const reducer = createReducer({
  [productVariantNew]: (state) => {
    return assoc(state,
      'sku', createEmptySku(),
      'err', null
    );
  },
  [skuClear]: state => {
    return dissoc(state, 'sku');
  },
  [syncSku]: (state, data) => {
    return update(state, 'sku', merge, data);
  },
  [_createProductVariant.succeeded]: updateProductVariantInState,
  [_updateProductVariant.succeeded]: updateProductVariantInState,
  [_fetchProductVariant.succeeded]: updateProductVariantInState,
}, initialState);

export default reducer;
