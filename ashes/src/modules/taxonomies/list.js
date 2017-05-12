// @flow

import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

export const rawSorts = ['name'];

const { reducer, actions } = makeLiveSearch(
  'taxonomies.list',
  searchTerms,
  'taxonomies_search_view/_search',
  'taxonomiesScope',
  {
    initialState: { sortBy: 'name' },
    rawSorts,
  }
);

export {
  reducer as default,
  actions,
};
