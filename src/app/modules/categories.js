/* @flow */

import { createReducer } from 'redux-act';
import createAsyncActions from './async-utils';

function apiCall(): Promise {
  const result = [
    {id: 2, name: 'eyeglasses'},
    {id: 3, name: 'sunglasses'},
    {id: 4, name: 'readers'},
  ];
  return Promise.resolve(result);
}

const initialState = {
  list: [],
};
const {fetch, ...actions} = createAsyncActions('categories', apiCall);

const reducer = createReducer({
  [actions.succeeded]: (state, payload) => {
    return {
      ...state,
      list: payload,
    };
  },
}, initialState);

export {
  reducer as default,
  fetch,
};
