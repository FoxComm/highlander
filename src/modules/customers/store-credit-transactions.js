
import makeLiveSearch from '../live-search';

const searchTerms = [];

const searches = [];

const { reducer, actions } = makeLiveSearch('STORE_CREDIT_TRANSACTIONS', searchTerms, searches);

export {
  reducer as default,
  actions
};
