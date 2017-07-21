/* @flow */

import { createReducer, createAction } from 'redux-act';
import _ from 'lodash';

import { createAsyncActions } from '@foxcomm/wings';

import type { Attributes, Sku, Album } from 'types/sku';

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
  // @TODO: get rid of this mock
  return this.api.get(`/v1/public/products/${id}`);
  /* .then((response) => {
    response.taxons.push({
      hierarchical: false,
      taxonomyId: 234251231,
      attributes: {
        name: {
          t: 'string',
          v: 'collection',
        },
      },
      taxons: [
        {
          id: 345342342346,
          taxonomyId: 234251231,
          attributes: {
            name: {
              v: 'Voyageur',
              t: 'string',
            },
          },
        },
      ],
    });
    _.each(response.skus, (sku) => {
      sku.attributes = {
        ...sku.attributes,
        weight: {
          t: 'string',
          v: '3',
        },
        height: {
          t: 'string',
          v: '15.5in',
        },
        width: {
          t: 'string',
          v: '13.25in',
        },
        depth: {
          t: 'string',
          v: '3.5in',
        },
      };
      sku.taxons = sku.taxons || [];
      sku.taxons.push({
        hierarchical: false,
        taxonomyId: 23425131,
        attributes: {
          name: {
            t: 'string',
            v: 'material',
          },
        },
        taxons: [
          {
            id: 34534242346,
            taxonomyId: 23425131,
            attributes: {
              name: {
                v: 'Textured Coated Canvas',
                t: 'string',
              },
            },
          },
        ],
      });
    });
    return response;
  }); */
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
