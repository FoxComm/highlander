import { combineReducers } from 'redux';
import group from './group';
import list from './list';


const groupReducer = combineReducers({
  group,
  list,
});

export default groupReducer;
