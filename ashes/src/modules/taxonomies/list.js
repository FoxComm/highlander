// @flow

import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const defaultSort = {
  rawSorts: ['name'],
};

const { reducer, actions } = makeLiveSearch(
  'taxonomies.list',
  searchTerms,
  'taxonomies_search_view/_search',
  'taxonomiesScope',
  defaultSort
);

export {
  reducer as default,
  actions,
};
