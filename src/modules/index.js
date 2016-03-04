
import { combineReducers } from 'redux';
import { routeReducer } from 'react-router-redux';
import {reducer as formReducer} from 'redux-form';
import cart from './cart';
import checkout from './checkout';
import categories from './categories';
import sidebar from './sidebar';

const reducer = combineReducers({
  routing: routeReducer,
  form: formReducer,
  categories,
  cart,
  checkout,
  sidebar,
});

export default reducer;
