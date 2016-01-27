import makeQuickSearch from '../quick-search';
import { toQuery } from '../../elastic/common';

const emptyFilters = [];
const emptyPhrase = "";
const { reducer, actions } = makeQuickSearch('order_sku', 'skus/_search', emptyFilters, emptyPhrase);

const suggestSkus = (phrase) => {
  return actions.doSearch(phrase);
};

const clearSkuSearch = () => {
  return actions.clearSearch();
};

export {
  suggestSkus,
  clearSkuSearch,
  reducer as default,
  actions
};
