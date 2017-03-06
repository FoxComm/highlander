/* @flow */

import { createAction, createReducer } from 'redux-act';
import createAsyncActions from './async-utils';
import { dissoc } from 'sprout-data';
import { api } from 'lib/api';

import type { asyncAction } from 'types';

// types

export type SignUpPayload = {
  name: string,
  email: string,
  password: string,
};

type LoginPayload = {
  email: string,
  password: string,
  org: string,
};

export const setUser = createAction('USER_SET');
export const logoutAction = createAction('AUTH_LOGOUT');
export const setJwt = createAction('AUTH_SET_JWT');

export const signUp = createAsyncActions('auth-signup', function signUp(payload: SignUpPayload): Promise {
  const {email, name, password} = payload;
  return api.auth.signup(email, name, password);
}).perform;

export const authenticate = createAsyncActions('auth-login', function authenticate(payload: LoginPayload): Promise {
  const {email, password, org} = payload;
  return api.auth.login(email, password, org)
    .then(({jwt, user}) => {
      this.dispatch(setJwt(jwt));
      this.dispatch(setUser(user));
    });
}).perform;

export function googleSignin(): asyncAction<void> {
  return () => {
    api.auth.googleSignin().then(urlInfo => {
      window.location.href = urlInfo.url;
    });
  };
}

export const logout = createAsyncActions('auth-logout', function logout(): Promise {
  return api.auth.logout()
    .then(() => {
      api.removeAuth();
      this.dispatch(logoutAction());
    });
}).perform;

const initialState = {
};

const reducer = createReducer({
  [setJwt]: (state, jwt) => {
    return {
      ...state,
      jwt,
    };
  },
  [setUser]: (state, user) => {
    return {
      ...state,
      user,
    };
  },
  [logoutAction]: (state) => {
    return dissoc(state, 'user', 'jwt');
  },
}, initialState);

export default reducer;
