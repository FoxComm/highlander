/* @flow */

import { createAction, createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import { assoc, dissoc, merge, update } from 'sprout-data';
import { api } from 'lib/api';
import { setUserId } from 'lib/analytics';
import { replace } from 'react-router-redux';

import type { asyncAction } from 'types';

// types

export type SignUpPayload = {
  firstName: string,
  lastName: string,
  email: string,
  password: string,
};

type LoginPayload = {
  email: string,
  password: string,
  kind: string,
};

export const toggleAuthMenu = createAction('AUTH_TOGGLE_MENU');
export const setUser = createAction('AUTH_SET_USER');
export const logoutAction = createAction('AUTH_LOGOUT');
export const setJwt = createAction('AUTH_SET_JWT');
export const updateUser = createAction('AUTH_UPDATE_USER');

export const signUp = createAsyncActions('auth-signup', function signUp(payload: SignUpPayload): Promise<*> {
  const {email, firstName, lastName, password} = payload;
  return api.auth.signup(email, firstName, lastName, password)
    .then(({jwt, user}) => {
      this.dispatch(setJwt(jwt));
      this.dispatch(setUser(user));
    });
}).perform;

export const authenticate = createAsyncActions('auth-login', function authenticate(payload: LoginPayload): Promise<*> {
  const {email, password, kind} = payload;
  return api.auth.login(email, password, kind)
    .then(({jwt, user}) => {
      this.dispatch(setJwt(jwt));
      this.dispatch(setUser(user));
    });
}).perform;

export const saveEmail = createAsyncActions('save-email', function saveEmail(email: string): Promise<*> {
  return api.account.update({email})
    .then((user) => {
      this.dispatch(setUser(user));
    });
}).perform;

export function googleSignin(): asyncAction<void> {
  return () => {
    api.auth.googleSignin().then((urlInfo) => {
      window.location.href = urlInfo.url;
    });
  };
}

export const logout = createAsyncActions('auth-logout', function logout(): Promise<*> {
  const { getState, dispatch } = this;
  return api.auth.logout()
    .then(() => {
      const { routing } = getState();
      api.removeAuth();
      this.dispatch(logoutAction());
      if (routing && routing.location.pathname.startsWith('/profile')) {
        dispatch(replace('/'));
      }
    });
}).perform;

export const fetchUser = createAsyncActions('fetch-user', function fetchUser(): Promise<*> {
  return api.account.get()
    .then((user) => {
      this.dispatch(setUser(user));
    });
}).perform;

export const restorePassword = createAsyncActions('restore-password',
  function restorePassword(email: string): Promise<*> {
    return api.auth.restorePassword(email);
  }
).perform;

export const resetPassword = createAsyncActions('reset-password',
  function resetPassword(code: string, password: string): Promise<*> {
    return api.auth.resetPassword(code, password);
  }
).perform;

const initialState = {
  isVisible: false,
};

const reducer = createReducer({
  [toggleAuthMenu]: state => assoc(state, 'isVisible', !state.isVisible),
  [setJwt]: (state, jwt) => {
    return {
      ...state,
      jwt,
    };
  },
  [setUser]: (state, user) => {
    setUserId(user.id);
    return {
      ...state,
      user,
    };
  },
  [updateUser]: (state, data) => {
    return update(state,
      'user', user => merge(user, data)
    );
  },
  [logoutAction]: (state) => {
    return dissoc(state, 'user', 'jwt');
  },
}, initialState);

export default reducer;
