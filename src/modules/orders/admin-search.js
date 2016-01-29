import makeQuickSearch from '../quick-search';
import { toQuery } from '../../elastic/common';

const emptyFilters = [];
const emptyPhrase = '';
const { reducer, actions: { doSearch } } = makeQuickSearch(
  'order_store_admins',
  'store_admins_search_view/_search',
  emptyFilters,
  emptyPhrase
);

export {
  reducer as default,
  doSearch
};
