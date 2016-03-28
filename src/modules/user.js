/** @flow */
import { createAction, createReducer } from 'redux-act';
import fetch from 'isomorphic-fetch';
import Api from '../lib/api';
import _ from 'lodash';

// types

export type TUser = {name: String, email: String};

export type LoginPayload = {
  email: string,
  password: string,
  kind: string,
};

export type UserState = {
  err: ?String,
  current: ?TUser,
  isFetching: boolean,
};

export const setUser = createAction('USER_SET');
const authStart = createAction('USER_AUTH_START');
const authError = createAction('USER_AUTH_ERROR');

const requestMyInfoStart = createAction("USER_AUTH_INFO_START");
const receiveMyInfoError = createAction("USER_AUTH_INFO_RECEIVE_ERROR");

export function fetchUserInfo(): ActionDispatch {
  return (dispatch, getState) => {
    let user = getState().user.current;
    if (user && user.name) return;

    user = localStorage.getItem("user");
    if (user) {
      try {
        dispatch(setUser(JSON.parse(user)));
        return;
      } catch(e) {
      }
    }

    dispatch(requestMyInfoStart());
    Api.get(`/admin/info`)
      .then(
        info => dispatch(setUser(info)),
        err => dispatch(receiveMyInfoError(err))
      );
  };
}

export function authenticate(payload: LoginPayload): ActionDispatch {

  const headers = {'Content-Type': 'application/json;charset=UTF-8'};

  return dispatch => {
    let hasError = false;

    dispatch(authStart());
    return fetch(Api.apiURI('/public/login'), {
      method: 'POST',
      body: JSON.stringify(payload),
      credentials: 'same-origin',
      headers,
    }).then(response => {
      if (response.status == 200 && response.headers.get('jwt')) {
        localStorage.setItem('jwt', response.headers.get('jwt'));
      } else {
        hasError = true;
      }
      return response.json();
    }).then((token: TUser) => {
      if (token.email && token.name && !hasError) {
        localStorage.setItem('user', JSON.stringify(token));
        return dispatch(setUser(token));
      }

      const errors = _.get(token, 'errors', ['Unexpected error, try again later']);
      const message = _.reduce(errors, (res, err) => `${res} ${err}`, '').trim();
      throw new Error(message);
    }).catch(reason => {
      dispatch(authError(reason.message));
      throw new Error(reason);
    });
  };
}

export function googleSignin(): ActionDispatch {
  return dispatch => {
    Api.get('/public/signin/google/admin').then(urlInfo => {
      window.location.href = urlInfo.url;
    });
  };
}

const initialState = {
  isFetching: false,
};

const reducer = createReducer({
  [requestMyInfoStart]: (state) => {
    return {...state,
      err: null,
      isFetching: true,
    };
  },
  [receiveMyInfoError]: (state, err) => {
    return {...state,
      isFetching: false,
      err: err,
    };
  },
  [setUser]: (state: UserState, user: TUser) => {
    localStorage.setItem("user", JSON.stringify(user));
    return {
      ...state,
      current: user,
      isFetching: false,
    };
  },
  [authStart]: (state: UserState) => {
    return {
      ...state,
      err: null,
      isFetching: true,
    };
  },
  [authError]: (state: UserState, error: string) => {
    return {
      ...state,
      err: error,
      isFetching: false,
    };
  },
}, initialState);

export default reducer;
