/**
 * @flow
 */

// data
import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';
import * as dsl from 'elastic/dsl';
import { addNativeFilters } from 'elastic/common';

const { reducer, actions } = makeLiveSearch(
  'products.list',
  searchTerms,
  'products_search_view/_search',
  'productsScope',
  {
    rawSorts: ['title'],
    //extraFilters: [dsl.existsFilter('archivedAt', 'missing')],
    skipInitialFetch: true
  }
);

export {
  reducer as default,
  actions
};
