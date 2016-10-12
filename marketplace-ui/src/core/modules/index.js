import { get } from 'lodash';
import { combineReducers } from 'redux';
import { routerReducer } from 'react-router-redux';
import { reducer as formReducer } from 'redux-form';

import {
  reducer as asyncReducer,
  inProgressSelector,
  failedSelector,
  succeededSelector,
  fetchedSelector,
} from './async-utils';

import * as application from './merchant-application';
import * as account from './merchant-account';
import * as info from './merchant-info';
import * as feed from './products-feed';
import * as shipping from './shipping-solution';

const reducer = combineReducers({
  routing: routerReducer,
  asyncActions: asyncReducer,
  form: formReducer,
  application: application.default,
  accounts: account.default,
  info: info.default,
  feed: feed.default,
  shipping: shipping.default,
});

export default reducer;

/** selectors */

export const getApplication = state => application.getApplication(state.application);
export const getApplicationApproved = state => application.getApplicationApproved(state.application);
export const getAccounts = state => account.getAccounts(state.accounts);
export const getInfo = state => info.getInfo(state.info);
export const getFeed = state => feed.getFeed(state.feed);
export const getShipping = state => shipping.getShipping(state.shipping);

const asyncSelector = namespace => selector => state => selector(state.asyncActions, namespace);

const applicationFetchSelector = asyncSelector(application.ACTION_FETCH);
const applicationSubmitSelector = asyncSelector(application.ACTION_SUBMIT);
const accountFetchSelector = asyncSelector(account.ACTION_FETCH);
const accountSubmitSelector = asyncSelector(account.ACTION_SUBMIT);
const infoFetchSelector = asyncSelector(info.ACTION_FETCH);
const infoSubmitSelector = asyncSelector(info.ACTION_SUBMIT);
const feedFetchSelector = asyncSelector(feed.ACTION_FETCH);
const feedSubmitSelector = asyncSelector(feed.ACTION_SUBMIT);
const feedUploadSelector = asyncSelector(feed.ACTION_UPLOAD);
const shippingFetchSelector = asyncSelector(shipping.ACTION_SUBMIT);
const shippingSubmitSelector = asyncSelector(shipping.ACTION_SUBMIT);

export const getApplicationFetched = applicationFetchSelector(fetchedSelector);
export const getApplicationFetchFailed = applicationFetchSelector(failedSelector);
export const getApplicationSubmitInProgress = applicationSubmitSelector(inProgressSelector);
export const getApplicationSubmitFailed = applicationSubmitSelector(failedSelector);

export const getAccountsFetched = accountFetchSelector(fetchedSelector);
export const getAccountSubmitInProgress = accountSubmitSelector(inProgressSelector);
export const getAccountSubmitFailed = accountSubmitSelector(failedSelector);

export const getInfoFetched = infoFetchSelector(fetchedSelector);
export const getInfoSubmitInProgress = infoSubmitSelector(inProgressSelector);
export const getInfoSubmitFailed = infoSubmitSelector(failedSelector);
export const getInfoSubmitSucceeded = infoSubmitSelector(succeededSelector);

export const getFeedFetched = feedFetchSelector(fetchedSelector);
export const getFeedSubmitInProgress = feedSubmitSelector(inProgressSelector);
export const getFeedSubmitFailed = feedSubmitSelector(failedSelector);
export const getFeedUploadInProgress = feedUploadSelector(inProgressSelector);
export const getFeedUploadFailed = feedUploadSelector(failedSelector);

export const getShippingFetched = shippingFetchSelector(fetchedSelector);
export const getShippingSubmitInProgress = shippingSubmitSelector(inProgressSelector);
export const getShippingSubmitFailed = shippingSubmitSelector(failedSelector);
