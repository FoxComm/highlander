/* @flow */

import { createReducer } from 'redux-act';
import createAsyncActions from './async-utils';

function apiCall(): Promise {
  const result = [
    {
      id: 0,
      name: 'ALL',
      description: '',
      imageUrl: '',
    },
    {
      id: 1,
      name: 'APPETIZERS',
      description: 'Dinner in 30 minutes',
      imageUrl: '',
    },
    {
      id: 2,
      name: 'ENTRÉES',
      description: 'Dinner in 30 minutes',
      imageUrl: '/images/categories/Cat_Entrees_2x.jpg',
    },
    {
      id: 3,
      name: 'BEST SELLERS',
      description: 'Dinner in 30 minutes',
      imageUrl: '/images/categories/Cat_Best_Sellers_2x.jpg',
    },
    {
      id: 4,
      name: 'GIFT CARDS',
      description: 'Gift cards will be here',
      imageUrl: '',
    },
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
