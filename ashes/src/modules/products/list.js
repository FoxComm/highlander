/**
 * @flow
 */

// data
import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const { reducer, actions } = makeLiveSearch(
  'products.list',
  searchTerms,
  'products_search_view',
  'productsScope',
  {
    rawSorts: ['title'],
  }
);

export {
  reducer as default,
  actions
};
