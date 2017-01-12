import { combineReducers } from 'redux';
// import dynamic from './dynamic';
import list from './list';
import details from './details';


const reducer = combineReducers({
  // dynamic,
  list,
  details,
});

export default reducer;
