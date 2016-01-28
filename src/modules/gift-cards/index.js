import { combineReducers } from 'redux';
import list from './list';
import adding from './new';
import details from './details';
import transactions from './transactions';

const giftCardReducer = combineReducers({
  list,
  adding,
  details,
  transactions
});

export default giftCardReducer;
