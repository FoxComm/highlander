// @flow

import { post } from 'lib/search';
import * as dsl from 'elastic/dsl';
import { createAsyncActions } from '@foxcomm/wings';
import { createReducer } from 'redux-act';

export type ProductVariant = {
  id: number;
  variantId: number,
  skuId: number,
  image: string|null,
  context: string,
  skuCode: string,
  title: string,
  salePrice: string,
  salePriceCurrency: string,
  retailPrice: string,
  retailPriceCurrency: string,
  archivedAt: string,
};


const _fetchProductVariants = createAsyncActions(
  'fetchProductVariants',
  function(productId: number) {
    const query = dsl.query({
      bool: {
        filter: [
          dsl.termFilter('productId', productId),
        ]
      }
    });

    return post('product_variants_search_view/_search', query);
  }
);
export const fetchProductVariants = _fetchProductVariants.perform;

const initialState = {
  list: [],
};

const reducer = createReducer({
  [_fetchProductVariants.succeeded]: (state, response) => {
    return {
      ...state,
      list: response.result,
    };
  },
}, initialState);

export default reducer;
