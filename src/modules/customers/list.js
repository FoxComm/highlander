import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const { reducer, actions } = makeLiveSearch('CUSTOMERS', searchTerms);

export {
  reducer as default,
  actions
};
