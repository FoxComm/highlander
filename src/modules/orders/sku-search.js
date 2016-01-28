import makeQuickSearch from '../quick-search';
import { toQuery } from '../../elastic/common';

const emptyFilters = [];
const emptyPhrase = "";
const { reducer, actions } = makeQuickSearch('order_sku', 'skus/_search', emptyFilters, emptyPhrase);

const suggestSkus = (phrase) => {
  return actions.doSearch(phrase);
};

export {
  suggestSkus,
  reducer as default,
  actions
};
