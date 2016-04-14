import { combineReducers } from 'redux';
import dynamic from './dynamic';
import list from './list';
import all from './all';


const reducer = combineReducers({
  dynamic,
  list,
  all,
});

export default reducer;
