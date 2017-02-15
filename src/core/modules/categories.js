/* @flow */

import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';

function apiCall(): Promise {
  const result = [
      {
        id: 0,
        name: 'APPETIZERS',
        description: 'Starters in 10 Minutes',
        imageUrl: '/images/categories/Cat_Appetizers_2x.jpg',
        showNameCatPage: true,
      },
      {
        id: 1,
        name: 'ENTRÉES',
        description: 'Dinner in 30 minutes',
        imageUrl: '/images/categories/Cat_Entrees_2x.jpg',
        showNameCatPage: true,
      },
      {
        id: 2,
        name: 'SIDES',
        description: '',
        imageUrl: '/images/categories/Cat_Sides_2x.jpg',
        showNameCatPage: true,
      },
      {
        id: 3,
        name: 'BEST SELLERS',
        description: 'Dinner in 30 minutes',
        imageUrl: '/images/categories/Cat_Best_Sellers_2x.jpg',
        showNameCatPage: true,
      },
      {
        id: 4,
        name: 'FAVORITES',
        description: 'Tried and True Favorites',
        imageUrl: '/images/categories/Cat_TriedTrue_2x.jpg',
        hiddenInNavigation: true,
        showNameCatPage: false,
      },
      {
        id: 5,
        name: 'GIFT CARDS',
        description: 'Gift cards will be here',
        imageUrl: '',
        showNameCatPage: true,
      },
      {
        // TODO: REMOVE THIS CODE AFTER Feb 15
        id: 6,
        name: 'VALENTINE',
        description: 'Valentine\'s Day Picks',
        imageUrl: '/images/categories/Cat_Valentine_2x.jpg',
        hiddenInNavigation: true,
        showNameCatPage: false,
      },
      {
        id: 7,
        name: 'WEEKNIGHT',
        description: 'Weeknight Favorites',
        imageUrl: '/images/categories/Cat_Weeknights_2x.jpg',
        hiddenInNavigation: true,
        showNameCatPage: false,
      },
      {
        id: 8,
        name: 'SPIN',
        description: 'Classics Revisited',
        imageUrl: '/images/categories/Cat_Spin_2x.jpg',
        hiddenInNavigation: true,
        showNameCatPage: false,
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
