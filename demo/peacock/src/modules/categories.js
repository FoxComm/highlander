/* @flow */

import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';

const featured = [
  {
    id: 100,
    name: 'New Arrivals',
  },
  {
    id: 101,
    name: 'Best Sellers',
  },
  {
    id: 102,
    name: 'Sale',
  },
  {
    id: 103,
    name: 'EQT',
  },
  {
    id: 104,
    name: 'Superstar',
  },
  {
    id: 105,
    name: 'Workout Essentials',
  },
  {
    id: 106,
    name: 'Blue Blast',
  },
];

const shoes = [
  {
    id: 201,
    name: 'Originals',
  },
  {
    id: 202,
    name: 'Soccer',
  },
  {
    id: 203,
    name: 'Running',
  },
  {
    id: 204,
    name: 'Basketball',
  },
  {
    id: 205,
    name: 'Training',
  },
  {
    id: 206,
    name: 'Outdoor',
  },
  {
    id: 207,
    name: 'Football',
  },
  {
    id: 208,
    name: 'Baseball',
  },
  {
    id: 209,
    name: 'Tennis',
  },
  {
    id: 210,
    name: 'Sandals & Slides',
  },
  {
    id: 211,
    name: 'NEO',
  },
];

const apparel = [
  {
    id: 301,
    name: 'Jackets',
  },
  {
    id: 302,
    name: 'Hoodies & Sweatshirts',
  },
  {
    id: 303,
    name: 'Track Suits & Warm Ups',
  },
  {
    id: 304,
    name: 'Long Sleeve Tops',
  },
  {
    id: 305,
    name: 'Short Sleeve Tops',
  },
  {
    id: 306,
    name: 'Graphic Tees',
  },
  {
    id: 307,
    name: 'Sleeveless Tops',
  },
  {
    id: 308,
    name: 'Jerseys',
  },
  {
    id: 309,
    name: 'Pants',
  },
  {
    id: 310,
    name: 'Tights',
  },
  {
    id: 311,
    name: 'Shorts',
  },
];

const accessories = [
  {
    id: 401,
    name: 'Bags',
  },
  {
    id: 402,
    name: 'Balls',
  },
  {
    id: 403,
    name: 'Sunglasses',
  },
  {
    id: 404,
    name: 'Watches',
  },
  {
    id: 405,
    name: 'Gloves',
  },
  {
    id: 406,
    name: 'Hats',
  },
  {
    id: 407,
    name: 'Socks',
  },
  {
    id: 408,
    name: 'Underwear',
  },
  {
    id: 409,
    name: 'Scarves',
  },
];

const sports = [
  {
    id: 501,
    name: 'Soccer',
  },
  {
    id: 502,
    name: 'Running',
  },
  {
    id: 503,
    name: 'Basketball',
  },
  {
    id: 504,
    name: 'Training',
  },
  {
    id: 505,
    name: 'Football',
  },
  {
    id: 506,
    name: 'Baseball',
  },
  {
    id: 507,
    name: 'Tennis',
  },
  {
    id: 508,
    name: 'Outdoor',
  },
  {
    id: 509,
    name: 'Weightlifting',
  },
  {
    id: 510,
    name: 'Skateboarding',
  },
  {
    id: 511,
    name: 'Snowboarding',
  },
  {
    id: 512,
    name: 'Hockey',
  },
  {
    id: 513,
    name: 'Lacrosse',
  },
  {
    id: 514,
    name: 'Volleyball',
  },
];

const groups = [
  {
    id: 10,
    name: 'Featured',
    children: featured,
  },
  {
    id: 11,
    name: 'Shoes',
    children: shoes,
  },
  {
    id: 12,
    name: 'Apparel',
    children: apparel,
  },
  {
    id: 13,
    name: 'Accessories',
    children: accessories,
  },
  {
    id: 14,
    name: 'Sports',
    children: sports,
  },
];

const categories = [
  {
    id: 0,
    name: 'WOMEN',
    description: '',
    showNameCatPage: true,
    children: groups,
  },
  {
    id: 1,
    name: 'MEN',
    description: '',
    showNameCatPage: true,
    children: groups,
  },
  {
    id: 2,
    name: 'KIDS',
    description: '',
    showNameCatPage: true,
    children: groups,
  },
  {
    id: 3,
    name: 'SPORTS',
    description: '',
    showNameCatPage: true,
    children: groups,
  },
  {
    id: 4,
    name: 'BRANDS',
    description: '',
    showNameCatPage: true,
    children: groups,
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
