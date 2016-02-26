
import { combineReducers } from 'redux';
import { routeReducer } from 'react-router-redux';
import {reducer as formReducer} from 'redux-form';

const reducer = combineReducers({
  routing: routeReducer,
  form: formReducer,
});

export default reducer;
