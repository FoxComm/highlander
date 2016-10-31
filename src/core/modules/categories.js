/* @flow */

import { createReducer } from 'redux-act';
import createAsyncActions from './async-utils';

function apiCall(): Promise {
  const result = [
    {id: 1, name: 'APPETIZERS', description: 'Dinner in 30 minutes'},
    {id: 2, name: 'ENTRÉES', description: 'Dinner in 30 minutes'},
    {id: 3, name: 'BEST SELLERS', description: 'Dinner in 30 minutes'},
    {id: 4, name: 'GIFT CARDS', description: 'Gift cards will be here'},
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
