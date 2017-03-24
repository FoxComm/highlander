/* @flow */

import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import { addMustNotFilter, defaultSearch, termFilter, addNestedTermFilter } from 'lib/elastic';
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

const MAX_RESULTS = 1000;
const context = process.env.FIREBIRD_CONTEXT || 'default';
const GIFT_CARD_TAG = 'GIFT-CARD';

function apiCall(
  categoryNames: ?Array<string>,
  sorting: { direction: number, field: string },
  { ignoreGiftCards = true } = {}): Promise<*> {
  let payload = defaultSearch(context);

  const filteredCats = _.remove(categoryNames, cat => cat != null);
  _.forEach(filteredCats, (cat) => {
    if (cat != 'ALL') {
      payload = addNestedTermFilter(payload, 'taxonomies', 'taxonomies.taxons', cat);
    }
  });

  if (ignoreGiftCards) {
    const giftCardTerm = termFilter('tags', GIFT_CARD_TAG);
    payload = addMustNotFilter(payload, giftCardTerm);
  }

  const order = sorting.direction === -1 ? 'desc' : 'asc';
    // $FlowFixMe
  payload.sort = [{ [sorting.field]: { order } }];

  return this.api.post(`/search/public/products_catalog_view/_search?size=${MAX_RESULTS}`, payload);
}

function searchGiftCards() {
  return apiCall.call({ api }, [GIFT_CARD_TAG], { direction: 1, field: 'salesPrice' }, { ignoreGiftCards: false });
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
