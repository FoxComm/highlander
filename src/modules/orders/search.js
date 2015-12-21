import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const { reducer, actions } = makeLiveSearch('ORDERS', searchTerms);

export {
  reducer as default,
  actions
};
