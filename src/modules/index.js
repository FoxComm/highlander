
import { combineReducers } from 'redux';
import { routeReducer } from 'react-router-redux'

const reducer = combineReducers({
  routing: routeReducer,
});

export default reducer;
