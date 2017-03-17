import { combineReducers } from 'redux';

import taxon from './taxon';
import products from './products-list';
import productsSearch from './products-search';

const reducer = combineReducers({
  taxon,
  products,
  productsSearch,
});

export default reducer;
