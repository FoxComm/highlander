/* @flow weak */

// libs
import { api as foxApi } from '../lib/api';
import { createReducer, createAction } from 'redux-act';
import { createAsyncActions } from '@foxcommerce/wings';
import _ from 'lodash';

import { GIFT_CARD_TAG } from './products';

// types
export type CrossSellPoint = {
  custID: number,
  prodID: number,
  chanID: number,
};

export type RelatedProduct = {
  id: number,
  score: number,
};

export type RelatedProductResponse = {
  response: {products: Array<RelatedProduct>},
};

// const
export const MAX_CROSS_SELLS_RESULTS = 10;

// helpers
function elasticSearchProductsQuery(rpResponse): Object {
  let matchingProductIds = _.reduce(rpResponse.products, (result, product) => {
    if (product.score > 0) {
      result.push(product.id);
    }
    return result;
  }, []);
  matchingProductIds = _.slice(matchingProductIds, 0, MAX_CROSS_SELLS_RESULTS);

  const query = {
    query: {
      bool: {
        filter: [
          {
            term: {
              context: 'default',
            },
          },
          {
            terms: {
              productId: matchingProductIds,
            },
          },
        ],
        must_not: [
          {
            term: {
              tags: GIFT_CARD_TAG,
            },
          },
        ],
      },
    },
  };

  return { query, productsOrder: matchingProductIds };
}

// actions - private
const updateRelatedProductsOrder = createAction('CROSS_SELL_UPDATED_RELATED_PRODUCTS_ORDER',
  productsOrder => productsOrder
);

const _fetchRelatedProducts = createAsyncActions('relatedProducts',
  function(productFormId: number, channelId: number) {
    return this.api.crossSell.crossSellRelated(productFormId, channelId)
      .then((res) => {
        const { dispatch } = this;
        const payload = elasticSearchProductsQuery(res);
        dispatch(updateRelatedProductsOrder(payload.productsOrder));

        return this.api.post(
          `/search/public/products_catalog_view/_search?size=${MAX_CROSS_SELLS_RESULTS}`, payload.query
        );
      }
    );
  }
);

// actions - public
export const clearRelatedProducts = createAction('CROSS_SELL_CLEAR_RELATED_PRODUCTS');

export const train = (customerId: number, channelId: number, cartLineItemsSkus: Array<any>) => {
  const crossSellPoints = _.map(_(cartLineItemsSkus).map('productFormId').value(), (productFormId) => {
    return { custID: customerId, prodID: productFormId, chanID: channelId };
  });

  return foxApi.crossSell.crossSellTrain({ points: crossSellPoints });
};

export const fetchRelatedProducts = _fetchRelatedProducts.perform;

// redux
const initialState = {
  relatedProducts: {},
  relatedProductsOrder: [],
};

const reducer = createReducer({
  [_fetchRelatedProducts.succeeded]: (state, response) => {
    return {
      ...state,
      relatedProducts: response,
    };
  },
  [updateRelatedProductsOrder]: (state, productsOrder) => {
    return {
      ...state,
      relatedProductsOrder: productsOrder,
    };
  },
  [clearRelatedProducts]: (state) => {
    return {
      ...state,
      relatedProducts: initialState.relatedProducts,
      relatedProductsOrder: initialState.relatedProductsOrder,
    };
  },
}, initialState);

export default reducer;
