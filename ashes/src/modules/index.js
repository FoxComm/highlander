import { routerReducer } from 'react-router-redux';
import { combineReducers } from 'redux';

import { reducer as asyncReducer } from './async-utils';
import giftCards from './gift-cards';
import customers from './customers';
import customerGroups from './customer-groups';
import carts from './carts';
import orders from './orders';
import rmas from './rmas';
import notes from './notes';
import countries from './countries';
import regions from './regions';
import addressForm from './address-form';
import reasons from './reasons';
import activityNotifications from './activity-notifications';
import storeCreditTypes from './store-credit-types';
import activityTrail from './activity-trail';
import insights from './insights';
import siteMenu from './site-menu';
import inventory from './inventory';
import products from './products';
import skus from './skus';
import user from './user';
import usermenu from './usermenu';
import promotions from './promotions';
import coupons from './coupons';
import users from './users';
import plugins from './plugins';
import objectSchemas from './object-schema';
import applications from './merchant-applications';
import originIntegrations from './origin-integrations';

const rootReducer = combineReducers({
  routing: routerReducer,
  asyncActions: asyncReducer,
  giftCards,
  customers,
  carts,
  orders,
  rmas,
  notes,
  user,
  usermenu,
  countries,
  regions,
  addressForm,
  reasons,
  storeCreditTypes,
  customerGroups,
  activityNotifications,
  activityTrail,
  insights,
  siteMenu,
  inventory,
  products,
  skus,
  promotions,
  coupons,
  users,
  plugins,
  objectSchemas,
  applications,
  originIntegrations,
});

export default rootReducer;
