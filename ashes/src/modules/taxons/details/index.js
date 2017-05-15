import { combineReducers } from 'redux';

import taxon from './taxon';
import products from './products-list';
import bulk from './bulk';

const reducer = combineReducers({
  taxon,
  bulk,
  products,
});

export default reducer;
