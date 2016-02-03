
import makeLiveSearch from '../live-search';

const searchTerms = [];

const { reducer, actions } = makeLiveSearch(
  'customers-storeCredits',
  searchTerms,
  'store_credits_search_view/_search',
  'customerStoreCreditsScope'
);

export {
  reducer as default,
  actions
};
