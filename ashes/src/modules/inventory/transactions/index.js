import { combineReducers } from 'redux';

import list from './transactions';
import bulk from './bulk';

const transactionsReducer = combineReducers({
  list,
  bulk,
});

export default transactionsReducer;
