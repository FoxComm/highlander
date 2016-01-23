import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const { reducer, actions } = makeLiveSearch('orders', searchTerms, 'ordersScope');

export {
  reducer as default,
  actions
};
