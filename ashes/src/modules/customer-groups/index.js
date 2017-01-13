import { combineReducers } from 'redux';
import list from './list';
import details from './details';

const reducer = combineReducers({
  list,
  details,
});

export default reducer;
