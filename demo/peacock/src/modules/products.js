/* @flow */

import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import { addMustNotFilter, defaultSearch, termFilter, addNestedTermFilter, addTermFilter } from 'lib/elastic';
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
};

export const MAX_RESULTS = 1000;
const context = process.env.FIREBIRD_CONTEXT || 'default';
export const GIFT_CARD_TAG = 'GIFT-CARD';

function apiCall(
  categoryNames: ?Array<string>,
  sorting: ?{ direction: number, field: string },
  { ignoreGiftCards = true } = {}): Promise<*> {
  let payload = defaultSearch(context);

  _.forEach(_.compact(categoryNames), (cat) => {
    if (cat !== 'ALL' && cat !== GIFT_CARD_TAG) {
      payload = addNestedTermFilter(payload, 'taxonomies', 'taxonomies.taxons', cat);
    } else if (cat === GIFT_CARD_TAG) {
      const tagTerm = termFilter('tags', cat.toUpperCase());
      payload = addTermFilter(payload, tagTerm);
    }
  });

  if (ignoreGiftCards) {
    const giftCardTerm = termFilter('tags', GIFT_CARD_TAG);
    payload = addMustNotFilter(payload, giftCardTerm);
  }

  if (sorting) {
    const order = sorting.direction === -1 ? 'desc' : 'asc';
    // $FlowFixMe
    payload.sort = [{ [sorting.field]: { order } }];
  }

  return this.api.post(`/search/public/products_catalog_view/_search?size=${MAX_RESULTS}`, payload);
}

function searchGiftCards() {
  return apiCall.call({ api }, [GIFT_CARD_TAG], null, { ignoreGiftCards: false });
}

const {fetch, ...actions} = createAsyncActions('products', apiCall);

const initialState = {
  list: [],
};

const reducer = createReducer({
  [actions.succeeded]: (state, payload) => {
    const result = _.isEmpty(payload.result) ? [] : payload.result;
    return {
      ...state,
      list: result,
    };
  },
}, initialState);

export {
  reducer as default,
  fetch,
  searchGiftCards,
};
