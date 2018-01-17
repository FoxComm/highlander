/* eslint-disable import/no-named-as-default */

import { combineReducers } from 'redux';
import { routeReducer } from 'react-router-redux';
import cart from './cart';
import checkout from './checkout';
import categories from './categories';
import sidebar from './sidebar';
import products from './products';
import productDetails from './product-details';
import countries from './countries';
import auth from './auth';
import profile from './profile';
import orders from './orders';
import reviews from './reviews';
import crossSell from './cross-sell';
import contentOverlay from './content-overlay';

import { reducer as asyncReducer } from '@foxcommerce/wings/lib/redux/async-utils';

const reducer = combineReducers({
  routing: routeReducer,
  asyncActions: asyncReducer,
  categories,
  cart,
  checkout,
  sidebar,
  products,
  productDetails,
  countries,
  auth,
  profile,
  orders,
  reviews,
  crossSell,
  contentOverlay,
});

export default reducer;
