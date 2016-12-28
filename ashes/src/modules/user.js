/** @flow */
import { createAction, createReducer } from 'redux-act';
import superagent from 'superagent';
import Api from '../lib/api';
import _ from 'lodash';
import { dissoc } from 'sprout-data';
import { createAsyncActions } from '@foxcomm/wings';

// types

export type TUser = {
  id: number,
  name: string,
  email: string,
  scopes: Array<string>,
};

export type LoginPayload = {
  email: string,
  password: string,
  org: string,
};

export type SignupPayload = {
  password: string,
  token: string,
}

export type UserState = {
  message: ?String,
  err: ?String,
  current: ?TUser,
};

export const setUser = createAction('USER_SET');
export const removeUser = createAction('REMOVE_SET');
export const authMessage = createAction('USER_AUTH_MESSAGE');

export const INFO_MESSAGES = {
  LOGGED_OUT: 'You have successfully logged out.',
};

export function fetchUserInfo(): ActionDispatch {
  return (dispatch, getState) => {
    let user = getState().user.current;
    if (user && user.name) return;

    user = localStorage.getItem('user');
    if (user) {
      try {
        dispatch(setUser(JSON.parse(user)));
      } catch(e) {
      }
    }
  };
}

function handleAuthResponse(dispatch, response) {
  const token: TUser = response.body;

  if (response.status == 200 && response.header['jwt']) {
    localStorage.setItem('jwt', response.header['jwt']);

    if (token.email) {
      return dispatch(setUser(token));
    }
  }

  throw new Error('Unexpected error, try again later');
}

const _authenticate = createAsyncActions(
  'authenticate',
  function(payload: LoginPayload) {
    const {dispatch} = this;

    return superagent.post(Api.apiURI('/public/login'), payload)
      .type('json')
      .then(response => handleAuthResponse(dispatch, response));
  }
);

export const authenticate = _authenticate.perform;

const _signUp = createAsyncActions(
  'signup',
  function(payload: SignupPayload) {
    const {dispatch} = this;

    return superagent.post(Api.apiURI('/public/signup'), payload)
      .type('json')
      .then(response => handleAuthResponse(dispatch, response));
  }
);

export const signUp = _signUp.perform;

export function googleSignin(): ActionDispatch {
  return dispatch => {
    Api.get('/public/signin/google/admin').then(urlInfo => {
      window.location.href = urlInfo.url;
    });
  };
}

export function logout(): ActionDispatch {
  return dispatch => {
    return Api.post('/public/logout').then(() => {
      localStorage.removeItem('user');
      localStorage.removeItem('jwt');
      dispatch(removeUser());
    });
  };
}

const initialState = {
  message: null,
};

function saveUser(state: UserState, user: TUser) {
  localStorage.setItem('user', JSON.stringify(user));
  return {
    ...state,
    current: user
  };
}

const reducer = createReducer({
  [setUser]: saveUser,
  [removeUser]: (state: UserState, user: TUser) => {
    return dissoc(state, 'current');
  },
  [_authenticate.started]: (state: UserState) => {
    return {
      ...state,
      message: null,
    };
  },
  [authMessage]: (state: UserState, message: string) => {
    return {
      ...state,
      message,
    };
  },
}, initialState);

export default reducer;
