/* @flow */

import { createReducer, createAction } from 'redux-act';
import _ from 'lodash';

import { createAsyncActions } from '@foxcommerce/wings';

type Attribute = {
  t: string,
  v: any,
};

type Attributes = { [key:string]: Attribute };

type Image = {
  alt?: string,
  src: string,
  title?: string,
};

type Album = {
  name: string,
  images: Array<Image>,
};

export type Sku = {
  id?: number,
  attributes: Attributes,
  albums: Array<Album>,
};

type Context = {
  name: string,
  attributes: { [key:string]: string },
};

export type VariantValue = {
  id: number,
  name: string,
  image: string,
  swatch: string,
  skuCodes: Array<string>,
}

export type ProductVariant = {
  attributes: {
    name: { t: string, v: string},
    type: { t: string, v: string},
  },
  values: Array<VariantValue>,
  id: number,
}


export type ProductResponse = {
  id: number,
  slug?: string,
  context: Context,
  attributes: Attributes,
  skus: Array<Sku>,
  albums: Array<Album>,
  variants: Array<ProductVariant>
};

export type ProductSlug = string|number;

export function getNextId(current: number): Function {
  return (dispatch, getState) => {
    const state = getState();
    const products = state.products.list;
    const idx = _.findIndex(products, {id: current});

    if (idx === -1) {
      return null;
    }

    const nextIdx = idx === (_.size(products) - 1) ? 0 : (idx + 1);
    return _.get(products, [nextIdx, 'id'], null);
  };
}

export function getPreviousId(current: number): Function {
  return (dispatch, getState) => {
    const state = getState();
    const products = state.products.list;
    const idx = _.findIndex(products, {id: current});

    if (idx === -1) {
      return null;
    }

    const previousIdx = idx < 1 ? (_.size(products) - 1) : (idx - 1);
    return _.get(products, [previousIdx, 'id'], null);
  };
}

function fetchProduct(id: ProductSlug): Promise<*> {
  return this.api.get(`/v1/public/products/${id}`);
}

const {fetch, ...actions} = createAsyncActions('pdp', fetchProduct);

export const resetProduct = createAction('RESET_PRODUCT');

const initialState = {
  product: null,
};

const reducer = createReducer({
  [actions.succeeded]: (state, payload) => ({
    ...state,
    product: payload,
  }),
  [actions.failed]: state => ({
    ...state,
    product: null,
  }),
  [resetProduct]: state => ({
    ...state,
    product: null,
  }),
}, initialState);

export {
  fetch,
  reducer as default,
};
