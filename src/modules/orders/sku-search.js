import makeQuickSearch from '../quick-search';
import { toQuery } from '../../elastic/common';

const emptyFilters = [];
const emptyPhrase = '';
const { reducer, actions } = makeQuickSearch('orders.skuSearch', 'skus/_search', emptyFilters, emptyPhrase);

const suggestSkus = (phrase) => {
  return actions.fetch(phrase);
};

export {
  suggestSkus,
  reducer as default,
  actions
};
