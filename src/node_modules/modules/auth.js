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
  kind: string,
};

export const setUser = createAction('USER_SET');
export const removeUser = createAction('REMOVE_USER');
export const setJwt = createAction('AUTH_SET_JWT');

export const signUp = createAsyncActions('auth-signup', function signUp(payload: SignUpPayload): Promise {
  const {email, name, password} = payload;
  return api.signup(email, name, password);
}).perform;

export const authenticate = createAsyncActions('auth-login', function authenticate(payload: LoginPayload): Promise {
  const {email, password, kind} = payload;
  return api.login(email, password, kind)
    .then(({jwt, user}) => {
      this.dispatch(setJwt(jwt));
      this.dispatch(setUser(user));
    });
}).perform;

export function googleSignin(): asyncAction<void> {
  return () => {
    api.googleSignin().then(urlInfo => {
      window.location.href = urlInfo.url;
    });
  };
}

export const logout = createAsyncActions('auth-logout', function logout(): Promise {
  return api.logout()
    .then(() => {
      this.dispatch(removeUser());
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
  [removeUser]: (state) => {
    return dissoc(state, 'user');
  },
}, initialState);

export default reducer;
