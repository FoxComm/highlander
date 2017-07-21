/* eslint-disable import/no-named-as-default */

import { combineReducers } from 'redux';
import { routeReducer } from 'react-router-redux';
import ui from './ui';
import cart from './cart';
import checkout from './checkout';
import categories from './categories';
import products from './products';
import productDetails from './product-details';
import countries from './countries';
import search from './search';
import auth from './auth';
import profile from './profile';
import orders from './orders';
import crossSell from './cross-sell';
import inventory from './inventory';

import { reducer as asyncReducer } from '@foxcomm/wings/lib/redux/async-utils';

const reducer = combineReducers({
  routing: routeReducer,
  asyncActions: asyncReducer,
  categories,
  cart,
  checkout,
  products,
  productDetails,
  countries,
  search,
  auth,
  profile,
  orders,
  crossSell,
  inventory,
  ui,
});

export default reducer;
