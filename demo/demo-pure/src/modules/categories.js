/* @flow */

import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';

const featured = [
  {
    id: 100,
    name: 'new arrivals',
  },
  {
    id: 101,
    name: 'best sellers',
  },
  {
    id: 102,
    name: 'sale',
  },
  {
    id: 103,
    name: 'EQT',
  },
  {
    id: 104,
    name: 'superstar',
  },
  {
    id: 105,
    name: 'workout essentials',
  },
  {
    id: 106,
    name: 'blue blast',
  },
];

const shoes = [
  {
    id: 201,
    name: 'originals',
  },
  {
    id: 202,
    name: 'soccer',
  },
  {
    id: 203,
    name: 'running',
  },
  {
    id: 204,
    name: 'basketball',
  },
  {
    id: 205,
    name: 'training',
  },
  {
    id: 206,
    name: 'outdoor',
  },
  {
    id: 207,
    name: 'football',
  },
  {
    id: 208,
    name: 'baseball',
  },
  {
    id: 209,
    name: 'tennis',
  },
  {
    id: 210,
    name: 'sandals & slides',
  },
  {
    id: 211,
    name: 'NEO',
  },
];

const apparel = [
  {
    id: 301,
    name: 'jackets',
  },
  {
    id: 302,
    name: 'hoodies & sweatshirts',
  },
  {
    id: 303,
    name: 'track suits & warm ups',
  },
  {
    id: 304,
    name: 'long sleeve tops',
  },
  {
    id: 305,
    name: 'short sleeve tops',
  },
  {
    id: 306,
    name: 'graphic tees',
  },
  {
    id: 307,
    name: 'sleeveless tops',
  },
  {
    id: 308,
    name: 'jerseys',
  },
  {
    id: 309,
    name: 'pants',
  },
  {
    id: 310,
    name: 'tights',
  },
  {
    id: 311,
    name: 'shorts',
  },
];

const accessories = [
  {
    id: 401,
    name: 'bags',
  },
  {
    id: 402,
    name: 'balls',
  },
  {
    id: 403,
    name: 'sunglasses',
  },
  {
    id: 404,
    name: 'watches',
  },
  {
    id: 405,
    name: 'gloves',
  },
  {
    id: 406,
    name: 'hats',
  },
  {
    id: 407,
    name: 'socks',
  },
  {
    id: 408,
    name: 'underwear',
  },
  {
    id: 409,
    name: 'scarves',
  },
];

const sports = [
  {
    id: 501,
    name: 'soccer',
  },
  {
    id: 502,
    name: 'running',
  },
  {
    id: 503,
    name: 'basketball',
  },
  {
    id: 504,
    name: 'training',
  },
  {
    id: 505,
    name: 'football',
  },
  {
    id: 506,
    name: 'baseball',
  },
  {
    id: 507,
    name: 'tennis',
  },
  {
    id: 508,
    name: 'outdoor',
  },
  {
    id: 509,
    name: 'weightlifting',
  },
  {
    id: 510,
    name: 'skateboarding',
  },
  {
    id: 511,
    name: 'snowboarding',
  },
  {
    id: 512,
    name: 'hockey',
  },
  {
    id: 513,
    name: 'lacrosse',
  },
  {
    id: 514,
    name: 'volleyball',
  },
];

const groups = [
  {
    id: 10,
    name: 'featured',
    children: featured,
  },
  {
    id: 11,
    name: 'shoes',
    children: shoes,
  },
  {
    id: 12,
    name: 'apparel',
    children: apparel,
  },
  {
    id: 13,
    name: 'accessories',
    children: accessories,
  },
  {
    id: 14,
    name: 'sports',
    children: sports,
  },
];

const categories = [
  {
    id: 0,
    name: 'women',
    description: '',
    showNameCatPage: true,
    children: groups,
  },
  {
    id: 1,
    name: 'men',
    description: '',
    showNameCatPage: true,
    children: groups,
  },
  {
    id: 2,
    name: 'kids',
    description: '',
    showNameCatPage: true,
    children: groups,
  },
];

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
};
