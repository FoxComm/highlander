import { combineReducers } from 'redux';
import group from './group';
import list from './list';

const reducer = combineReducers({
  group,
  list,
});

export default reducer;
