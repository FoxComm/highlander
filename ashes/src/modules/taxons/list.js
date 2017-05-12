/* @flow */

import makeLiveSearch from 'modules/live-search';

const searchTerms = [];
const storeLocation = 'taxons.list';
const searchView = 'taxons_search_view/_search';
const scope = 'taxonsScope';

export const rawSorts = ['name'];

const { reducer, actions } = makeLiveSearch(
  storeLocation,
  searchTerms,
  searchView,
  scope,
  {
    initialState: { sortBy: 'name' },
    rawSorts,
  }
);

export {
  reducer as default,
  actions,
};
