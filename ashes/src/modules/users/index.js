import { combineReducers } from 'redux';
import list from './list';
import details from './details';

const userReducer = combineReducers({
  list,
  details,
});

export default userReducer;
