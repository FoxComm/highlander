
import { combineReducers } from 'redux';
import list from './list';
import bulk from './bulk';
import details from './details';
import watchers from './watchers';

const promotionsReducer = combineReducers({
  list,
  bulk,
  details,
  watchers,
});

export default promotionsReducer;
