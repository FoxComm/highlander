import makeQuickSearch from '../quick-search';

const emptyFilters = [];
const emptyPhrase = '';
const { reducer, actions } = makeQuickSearch('carts.skuSearch', 'sku_search_view/_search', emptyFilters, emptyPhrase);

const suggestSkus = (phrase) => {
  return actions.fetch(phrase);
};

export {
  suggestSkus,
  reducer as default,
  actions
};
