import { combineReducers } from 'redux';
import dynamic from './dynamic';
import list from './list';


const reducer = combineReducers({
  dynamic,
  list,
});

export default reducer;
