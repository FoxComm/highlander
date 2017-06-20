import { combineReducers } from 'redux';

import bulk from './bulk';
import list from './transactions';

const transactionsReducer = combineReducers({
  bulk,
  list,
});

export default transactionsReducer;
