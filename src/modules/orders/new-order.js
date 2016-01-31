import makeQuickSearch from '../quick-search';
import { toQuery } from '../../elastic/common';

const emptyFilters = [];
const emptyPhrase = '';
const { reducer, actions } = makeQuickSearch(
  'order_customers',
  'customers_search_view/_search',
  emptyFilters,
  emptyPhrase
);

const suggestCustomers = phrase => actions.doSearch(phrase);

export {
  reducer as default,
  suggestCustomers,
  actions
};
