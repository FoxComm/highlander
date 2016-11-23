/* @flow */

import { createReducer } from 'redux-act';
import { createAsyncActions } from 'wings';
import { addTermFilter, defaultSearch, termFilter } from 'lib/elastic';
import _ from 'lodash';
import { api } from 'lib/api';

export type Product = {
  id: number;
  context: string,
  title: string;
  description: string,
  images: ?Array<string>,
  currency: string,
  productId: number,
  salePrice: string,
  scope: string,
  skus: Array<string>,
  tags: Array<string>,
  albums: ?Array<Object> | Object,
}

const MAX_RESULTS = 1000;
const context = process.env.FIREBIRD_CONTEXT || 'default';
const GIFT_CARD_TAG = 'GIFT-CARD';

function apiCall(
  categoryName: ?string, productType: ?string, { ignoreGiftCards = true } = {}): global.Promise {
  let payload = defaultSearch(context);

  [categoryName, productType].forEach(tag => {
    // products do not have 'ALL' tag
    if (tag && tag.toUpperCase() !== 'ALL') {
      const tagTerm = termFilter('tags', tag.toUpperCase());
      payload = addTermFilter(payload, tagTerm);
    }
  });

  if (ignoreGiftCards) {
    const giftCardTerm = termFilter('tags', GIFT_CARD_TAG);
    payload = addTermFilter(payload, null, { mustNot: giftCardTerm });
  }

  return this.api.post(
    `/search/public/products_catalog_view/_search?size=${MAX_RESULTS}`, payload);
}

function searchGiftCards() {
  return apiCall.call({ api }, GIFT_CARD_TAG, null, { ignoreGiftCards: false });
}

const {fetch, ...actions} = createAsyncActions('products', apiCall);

const reducer = createReducer({
  [actions.succeeded]: (state, payload) => {
    const result = _.isEmpty(payload.result) ? [] : payload.result;
    return {
      ...state,
      list: result,
    };
  },
}, {list: []});

export {
  reducer as default,
  fetch,
  searchGiftCards,
};
