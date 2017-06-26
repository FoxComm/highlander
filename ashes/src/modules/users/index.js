import { combineReducers } from 'redux';
import list from './list';
import details from './details';
import bulk from './bulk';

const userReducer = combineReducers({
  list,
  bulk,
  details,
});

export default userReducer;
