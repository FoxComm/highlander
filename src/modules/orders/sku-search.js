import makeQuickSearch from '../quick-search';
import { toQuery } from '../../elastic/common';

const emptyFilters = [];
const emptyPhrase = "";
const { reducer, actions } = makeQuickSearch('order_sku', emptyFilters, emptyPhrase);

const suggestSkus = (phrase) => {
  return dispatch => {
    let filters = [];
    dispatch(actions.submitSearch(filters, phrase));
    const esQuery = toQuery(filters, phrase);
    dispatch(actions.fetch('skus/_search', esQuery.toJSON()));
  };
};

export {
  suggestSkus,
  reducer as default,
  actions
};
