/* @flow weak */

// libs
import { createReducer, createAction } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';

// types
export type CrossSellPoint = {
  custID: number,
  prodID: number,
  chanID: number,
};

export type RelatedProduct = {
  product: Object,
  score: number,
};

export type RelatedProductResponse = {
  response: {products: Array<RelatedProduct>},
};

// const
export const MAX_CROSS_SELLS_RESULTS = 8;

// actions - private
const _fetchRelatedProducts = createAsyncActions('relatedProducts',
  function(productFormId: number, channelId: number) {
    return this.api.crossSell.crossSellRelatedFull(productFormId, channelId, MAX_CROSS_SELLS_RESULTS);
  }
);

// actions - public
export const clearRelatedProducts = createAction('CROSS_SELL_CLEAR_RELATED_PRODUCTS');

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
  [clearRelatedProducts]: (state) => {
    return {
      ...state,
      relatedProducts: initialState.relatedProducts,
    };
  },
}, initialState);

export default reducer;
