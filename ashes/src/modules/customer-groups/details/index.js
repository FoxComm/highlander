import { combineReducers } from 'redux';

import group from './group';
import customers from './customers-list';
import bulk from './bulk';

const reducer = combineReducers({
  group,
  customers,
  bulk,
});

export default reducer;
