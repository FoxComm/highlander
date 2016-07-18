/**
 * @flow
 */

import makeLiveSearch from '../live-search';

const searchTerms = [];
const { reducer, actions } = makeLiveSearch(
  'skus.list',
  searchTerms,
  'sku_search_view/_search',
  'skusScope',
  {
    rawSorts: ['title']
  }
);

export {
  reducer as default,
  actions
};
