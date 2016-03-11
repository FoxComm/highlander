/**
 * @flow
 */

import makeLiveSearch from '../live-search';

const searchTerms = [];
const { reducer, actions } = makeLiveSearch(
  'products.list',
  searchTerms,
  'products_search_view/_search',
  'productsScope',
  {}
);

export {
  reducer as default,
  actions
};
