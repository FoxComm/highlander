import { combineReducers } from 'redux';
import list from './list';
import bulk from './bulk';
import adding from './new';
import details from './details';
import transactions from './transactions';

const contentTypeReducer = combineReducers({
  list,
  bulk,
  adding,
  details,
  transactions,
});

export default contentTypeReducer;
