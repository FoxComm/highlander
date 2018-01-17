/* @flow */

import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const { reducer, actions } = makeLiveSearch(
  'catalogs.list',
  searchTerms,
  'catalogs_search_view',
  'catalogsScope',
  {
    rawSorts: ['name'],
  },
);

export {
  reducer as default,
  actions
};
