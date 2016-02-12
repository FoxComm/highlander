
import { combineReducers } from 'redux';
import { routeReducer } from 'react-router-redux'
import cat from './cat';

const reducer = combineReducers({
  cat,
  routing: routeReducer,
});

export default reducer;
