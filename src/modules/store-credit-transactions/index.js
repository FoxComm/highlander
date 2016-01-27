import { combineReducers } from 'redux';
import list from './list';

const sctxReducer = combineReducers({
  list,
});

export default sctxReducer;
