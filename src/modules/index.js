import { routerStateReducer } from 'redux-router';
import { combineReducers } from 'redux';
import giftCards from './gift-cards';
import customers from './customers';
import groups from './groups';
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
import siteMenu from './site-menu';
import inventory from './inventory';
import expandableTables from './expandable-tables';

const rootReducer = combineReducers({
  router: routerStateReducer,
  giftCards,
  customers,
  orders,
  rmas,
  notes,
  countries,
  regions,
  addressForm,
  reasons,
  storeCreditTypes,
  groups,
  activityNotifications,
  activityTrail,
  siteMenu,
  inventory,
  expandableTables,
});

export default rootReducer;
