
import makeLiveSearch from '../live-search';

const searchTerms = [];

const searches = [];

const { reducer, actions } = makeLiveSearch('storeCreditTransactions', searchTerms, searches);

export {
  reducer as default,
  actions
};
