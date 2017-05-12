/**
 * @flow
 */

// data
import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

export const rawSorts = ['title'];

const { reducer, actions } = makeLiveSearch(
  'products.list',
  searchTerms,
  'products_search_view/_search',
  'productsScope',
  {
    rawSorts,
  }
);

export {
  reducer as default,
  actions
};
