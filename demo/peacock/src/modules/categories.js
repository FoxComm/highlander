/* @flow */

import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';

const categories = [
  {
    id: 0,
    name: 'Women',
    showNameCatPage: true,
  },
  {
    id: 1,
    name: 'Men',
    showNameCatPage: true,
  },
  {
    id: 2,
    name: 'Kids',
    showNameCatPage: true,
  },
  {
    id: 3,
    name: 'Sports',
    showNameCatPage: true,
  },
  {
    id: 4,
    name: 'Brands',
    showNameCatPage: true,
  },
  {
    id: 5,
    name: 'GIFT CARDS',
    description: 'Gift cards will be here',
    imageUrl: '',
    showNameCatPage: true,
  },
];

const productTypes = [
  'All',
  'Poultry',
  'Seafood',
  'Meat',
  'Vegetarian',
];

function convertCategoryNameToUrlPart(categoryName: string) {
  return encodeURIComponent(categoryName.replace(/\s/g, '-'));
}

const initialState = {
  list: [],
};
const {fetch, ...actions} = createAsyncActions(
  'categories',
  () => Promise.resolve(categories)
);

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
  categories,
  productTypes,
  convertCategoryNameToUrlPart,
};
