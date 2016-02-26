
import { combineReducers } from 'redux';
import { routeReducer } from 'react-router-redux';
import {reducer as formReducer} from 'redux-form';

import categories from './categories';

const reducer = combineReducers({
  routing: routeReducer,
  form: formReducer,
  categories,
});

export default reducer;
