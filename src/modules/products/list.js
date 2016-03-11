/**
 * @flow
 */

import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

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
