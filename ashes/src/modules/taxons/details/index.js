import { combineReducers } from 'redux';

import taxon from './taxon';
import list from './products-list';
import bulk from './bulk';

const reducer = combineReducers({
  taxon,
  bulk,
  list,
});

export default reducer;
