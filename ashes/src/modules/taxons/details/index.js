import { combineReducers } from 'redux';

import taxon from './taxon';
import products from './products-list';

const reducer = combineReducers({
  taxon,
  products,
});

export default reducer;
