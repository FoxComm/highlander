import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const { reducer, actions } = makeLiveSearch('customers', searchTerms);

export {
  reducer as default,
  actions
};
