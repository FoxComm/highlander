/* @flow */

import { createAction, createReducer } from 'redux-act';
import createAsyncActions from './async-utils';
import fetch from 'isomorphic-fetch';
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

const headers = {'Content-Type': 'application/json;charset=UTF-8'};

export const signUp = createAsyncActions('auth-signup', function signUp(payload: SignUpPayload): Promise {
  return api.post('/v1/public/registrations/new', payload);
}).perform;

export const authenticate = createAsyncActions('auth-login', function authenticate(payload: LoginPayload): Promise {
  return fetch('/api/v1/public/login', {
    method: 'POST',
    body: JSON.stringify(payload),
    credentials: 'same-origin',
    headers,
  })
    .then(response => {
      const jwt = response.headers.get('jwt');
      if (response.status == 200 && jwt) {
        localStorage.setItem('jwt', jwt);
        this.dispatch(setJwt(jwt));
        return response.json();
      }
      throw new Error('Server error, try again later. Sorry for inconvenience :(');
    })
    .then((token) => {
      if (token.email && token.name) {
        localStorage.setItem('user', JSON.stringify(token));
        this.dispatch(setUser(token));
        return;
      }
      throw new Error('Server error, try again later. Sorry for inconvenience :(');
    });
}).perform;

export function googleSignin(): asyncAction<void> {
  return () => {
    api.get('/v1/public/signin/google/customer').then(urlInfo => {
      window.location.href = urlInfo.url;
    });
  };
}

export const logout = createAsyncActions('auth-logout', function logout(): Promise {
  return api.post('/v1/public/logout')
    .then(() => {
      this.dispatch(removeUser());
      localStorage.removeItem('user');
    });
}).perform;

const initialState = {
  inProgress: false,
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
