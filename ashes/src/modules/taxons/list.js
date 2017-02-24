// @flow

import makeLiveSearch from '../live-search';

const defaultSort = {
  rawSorts: ['name'],
};

const searchTerms = [];
const storeLocation = 'taxons.list';
const searchView = 'taxons_search_view/_search';
const scope = 'taxonsScope';

const { reducer, actions } = makeLiveSearch(
  storeLocation,
  searchTerms,
  searchView,
  scope,
  defaultSort
);

export {
  reducer as default,
  actions,
};
