/* @flow */

import makeLiveSearch from 'modules/live-search';
import productsSearchTerms from 'modules/products/search-terms';

const { reducer, actions } = makeLiveSearch(
  'taxons.details.products',
  productsSearchTerms,
  'products_search_view',
  'productsScope',
  {
    initialState: { sortBy: 'name' },
    rawSorts: ['name'],
    skipInitialFetch: true,
  }
);

export {
  reducer as default,
  actions,
};
