import { combineReducers } from 'redux';
import list from './list';

const userReducer = combineReducers({
  list,
});

export default userReducer;
