/* @flow weak */

// libs
import { api as foxApi } from '../lib/api';
import { createReducer, createAction } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import _ from 'lodash';

import { GIFT_CARD_TAG, MAX_RESULTS } from './products';

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

// helpers
function elasticSearchProductsQuery(rpResponse): Object {
  const matchingProductIds = _.reduce(rpResponse.products, (result, product) => {
    if (product.score > 0) {
      result.push(product.id);
    }
    return result;
  }, []);

  const query = {
    "query": {
      "bool": {
        "filter": [
          {
            "term": {
              "context": "default"
            }
          },
          {
            "terms": {
              "productId": matchingProductIds
            }
          }
        ],
        "must_not": [
          {
            "term": {
              "tags": GIFT_CARD_TAG
            }
          }
        ]
      }
    }
  };

  return query;
}

// actions
const _fetchRelatedProducts = createAsyncActions('relatedProducts',
  function(productFormId: number, channelId: number) {
    return this.api.crossSell.crossSellRelated(productFormId, channelId)
      .then(res => {
        const payload = elasticSearchProductsQuery(res);
        return this.api.post(
          `/search/public/products_catalog_view/_search?size=${MAX_RESULTS}`, payload
        );
      }
    );
  }
);

export const clearRelatedProducts = createAction('CROSS_SELL_CLEAR_RELATED_PRODUCTS');

export const train = (customerId: number, channelId: number, cartLineItemsSkus: Array<any>) => {
  const crossSellPoints = _.map(_(cartLineItemsSkus).map('productFormId').value(), (productFormId) => {
    return { 'custID': customerId, 'prodID': productFormId, 'chanID': channelId };
  });

  return foxApi.crossSell.crossSellTrain({ 'points': crossSellPoints });
};

export const fetchRelatedProducts = _fetchRelatedProducts.perform;

// redux
const initialState = {
  relatedProducts: {},
};

const reducer = createReducer({
  [_fetchRelatedProducts.succeeded]: (state, response) => {
    return {
      ...state,
      relatedProducts: response,
    };
  },
  [clearRelatedProducts]: state => {
    return {
      ...state,
      relatedProducts: initialState.relatedProducts,
    };
  },
}, initialState);

export default reducer;
