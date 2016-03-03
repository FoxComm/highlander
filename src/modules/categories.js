/* @flow */

import { createAction, createReducer } from 'redux-act';

const categoriesFetchStated = createAction('CATEGORIES_FETCH_STARTED');
const categoriesFetchSucceded = createAction('CATEGORIES_FETCH_SUCCEDED');
const categoriesFetchFailed = createAction('CATEGORIES_FETCH_FAILED');

export function fetchCategories(): Function {
  return dispatch => {
    dispatch(categoriesFetchStated());

    const result = ['all', 'eyeglasses', 'sunglasses', 'readers'];
    return dispatch(categoriesFetchSucceded(result));
  };
}

type FormData = {
  isFetching: boolean;
  list: Array<string>;
}

const initialState: FormData = {
  isFetching: false,
  list: [],
};

const reducer = createReducer({
  [categoriesFetchStated]: state => {
    return {
      ...state,
      isFetching: true,
    };
  },
  [categoriesFetchSucceded]: (state, payload) => {
    return {
      ...state,
      list: payload,
      isFetching: false,
    };
  },
  [categoriesFetchFailed]: (state, err) => {
    console.error(err);

    return {
      ...state,
      isFetching: false,
    };
  },
}, initialState);

export default reducer;
