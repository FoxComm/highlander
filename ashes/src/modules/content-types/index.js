
import { combineReducers } from 'redux';
import list from './list';
import bulk from './bulk';
import details from './details';

const promotionsReducer = combineReducers({
  list,
  bulk,
  details,
});

export default promotionsReducer;
