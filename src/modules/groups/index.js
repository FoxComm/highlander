import { combineReducers } from 'redux';
import builder from './builder';
import list from './list';


const groupReducer = combineReducers({
  builder,
  list,
});

export default groupReducer;
