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

const reducer = combineReducers({
  routing: routerReducer,
  asyncActions: asyncReducer,
  form: formReducer,
  application: application.default,
  accounts: account.default,
  info: info.default,
});

export default reducer;

/** selectors */

export const getApplication = state => application.getApplication(state.application);
export const getAccounts = state => account.getAccounts(state.accounts);

export const getApplicationFetched = state => fetchedSelector(state.asyncActions, application.ACTION_FETCH);
export const getApplicationFetchFailed = state => failedSelector(state.asyncActions, application.ACTION_FETCH);
export const getApplicationSubmitInProgress = state => inProgressSelector(state.asyncActions, application.ACTION_SUBMIT);
export const getApplicationSubmitFailed = state => failedSelector(state.asyncActions, application.ACTION_SUBMIT);

export const getAccountsFetched = state => fetchedSelector(state.asyncActions, account.ACTION_FETCH);
export const getAccountSubmitInProgress = state => inProgressSelector(state.asyncActions, account.ACTION_SUBMIT);
export const getAccountSubmitFailed = state => failedSelector(state.asyncActions, account.ACTION_SUBMIT);
export const getAccountSubmitSucceeded = state => succeededSelector(state.asyncActions, account.ACTION_SUBMIT);

export const getInfoSubmitInProgress = state => inProgressSelector(state.asyncActions, info.ACTION_SUBMIT);
export const getInfoSubmitFailed = state => failedSelector(state.asyncActions, info.ACTION_SUBMIT);
export const getInfoSubmitSucceeded = state => succeededSelector(state.asyncActions, info.ACTION_SUBMIT);
