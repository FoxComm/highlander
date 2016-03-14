/** @flow */
import { createAction, createReducer } from 'redux-act';
import fetch from 'isomorphic-fetch';
import Api from '../lib/api';

// types

export type TUser = {name: String, email: String};

export type LoginPayload = {
  email: string,
  password: string,
  kind: string,
};


export const setUser = createAction('USER_SET');
const authenticationStart = createAction('USER_AUTH_START');


export function authenticate(payload: LoginPayload): ActionDispatch {
  const headers = {'Content-Type': 'application/json;charset=UTF-8'};
  return dispatch => {
    dispatch(authenticationStart());
    return fetch(Api.apiURI('/public/login'), {
      method: 'POST',
      body: JSON.stringify(payload),
      credentials: "same-origin",
      headers,
    }).then(response => {
      if (response.status == 200) {
        localStorage.setItem('jwt', response.headers.get('jwt'));
        return response.json();
      }
    }).then((token: TUser) => {
        localStorage.setItem('user', JSON.stringify(token));
        dispatch(setUser(token));
    });
  };
}

const initialState = {};

const reducer = createReducer({
  [setUser]: (state, user: TUser) => {
    return {
      ...state,
      current: user,
      isFetching: false,
    };
  },
  [authenticationStart]: (state: any) => {
    return {
      ...state,
      err: null,
      isFetching: true,
    };
  },
}, initialState);

export default reducer;
